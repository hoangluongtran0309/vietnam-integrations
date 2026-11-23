package io.github.hoangluongtran0309.vietnam.demo.capture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

final class CaptureSession implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureSession.class);
    private static final String PROPERTY = "demo.zalo.capture-dir";
    private static final DateTimeFormatter DIRECTORY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss")
            .withLocale(Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private final ObjectMapper mapper;
    private final CaptureRedactor redactor;
    private final Clock clock;
    private final Path directory;
    private final Instant startedAt;
    private final List<Account> accounts;
    private final AtomicInteger sequence = new AtomicInteger();

    CaptureSession(String configuredDirectory, ObjectMapper mapper, Clock clock, List<Account> accounts) {
        this.mapper = mapper;
        this.redactor = new CaptureRedactor(mapper);
        this.clock = clock;
        this.accounts = List.copyOf(accounts);
        this.startedAt = clock.instant();
        this.directory = initializeDirectory(configuredDirectory, startedAt);
        writeSession(false);
    }

    CaptureRedactor redactor() {
        return redactor;
    }

    Path directory() {
        return directory;
    }

    Instant now() {
        return clock.instant();
    }

    void writeCapture(String type, String suffix, ObjectNode capture) {
        String filename = "%04d-%s-%s.json".formatted(sequence.incrementAndGet(), type, slug(suffix));
        safeWrite(filename, capture);
    }

    void warn(String type, String filename) {
        LOGGER.warn("Zalo capture {} while writing {}", type, filename);
    }

    @Override
    public void close() {
        writeSession(true);
    }

    private void writeSession(boolean finished) {
        ObjectNode root = mapper.createObjectNode();
        root.put("startedAt", startedAt.toString());
        root.put("demoVersion", demoVersion());
        ArrayNode aliases = root.putArray("aliases");
        for (Account account : accounts) {
            ObjectNode node = aliases.addObject();
            node.put("alias", account.alias());
            putPseudonymOrNull(node, "oaId", account.oaId());
            putPseudonymOrNull(node, "appId", account.appId());
        }
        if (finished) {
            root.put("finishedAt", clock.instant().toString());
            root.set("defaultFields", mapper.valueToTree(redactor.defaultFields()));
        }
        safeWrite("session.json", root);
    }

    private void putPseudonymOrNull(ObjectNode node, String field, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(field);
            return;
        }
        node.put(field, redactor.pseudonymize(value));
    }

    private void safeWrite(String filename, JsonNode value) {
        Path target = directory.resolve(filename);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, ".capture-", ".tmp");
            setPermissions(temporary, FILE_PERMISSIONS);
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            setPermissions(target, FILE_PERMISSIONS);
        } catch (RuntimeException | IOException e) {
            warn(e.getClass().getSimpleName(), filename);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    warn(cleanupFailure.getClass().getSimpleName(), temporary.getFileName().toString());
                }
            }
        }
    }

    private static Path initializeDirectory(String configuredDirectory, Instant startedAt) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException(PROPERTY + " must not be blank when Zalo capture is enabled");
        }
        try {
            Path base = Path.of(configuredDirectory).toAbsolutePath().normalize();
            if (Files.exists(base) && !Files.isDirectory(base)) {
                throw new IllegalStateException(PROPERTY + " must point to a directory: " + base);
            }
            Files.createDirectories(base);
            base = base.toRealPath().normalize();
            rejectGitTree(base);
            setPermissions(base, DIRECTORY_PERMISSIONS);
            Path session = uniqueSessionDirectory(base, DIRECTORY_TIME.format(startedAt) + "-session");
            setPermissions(session, DIRECTORY_PERMISSIONS);
            return session;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize " + PROPERTY + ": " + e.getClass().getSimpleName(), e);
        }
    }

    private static void rejectGitTree(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.exists(current.resolve(".git"))) {
                throw new IllegalStateException(PROPERTY + " must be outside every directory containing .git");
            }
        }
    }

    private static Path uniqueSessionDirectory(Path base, String name) throws IOException {
        for (int attempt = 1; ; attempt++) {
            Path candidate = base.resolve(attempt == 1 ? name : name + "-" + attempt);
            try {
                return Files.createDirectory(candidate);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // A second demo process started in the same second; use a deterministic suffix.
            }
        }
    }

    private static void setPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(path, permissions);
        }
    }

    private static String slug(String value) {
        String slug = value.replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "root" : slug;
    }

    private static String demoVersion() {
        String version = CaptureSession.class.getPackage().getImplementationVersion();
        return version == null ? "development" : version;
    }

    record Account(String alias, String oaId, String appId) {
    }
}
