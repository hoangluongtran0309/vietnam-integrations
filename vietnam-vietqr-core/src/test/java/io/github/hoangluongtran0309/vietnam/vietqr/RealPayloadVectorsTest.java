package io.github.hoangluongtran0309.vietnam.vietqr;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Decodes every payload collected from real banking apps in {@code src/test/resources/vectors/*.properties}.
 * The file format is described in {@code docs/vietqr/test-vectors.md}.
 */
class RealPayloadVectorsTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            "source", "collectedOn", "masked", "payload",
            "expected.bankBin", "expected.accountNumber", "expected.dynamic");

    private final VietQrDecoder decoder = new VietQrDecoder();
    private final VietQrEncoder encoder = new VietQrEncoder();

    @TestFactory
    Stream<DynamicTest> realPayloads() throws IOException, URISyntaxException {
        URL directory = getClass().getResource("/vectors");
        if (directory == null) {
            return Stream.empty();
        }
        List<Path> files;
        try (Stream<Path> listing = Files.list(Path.of(directory.toURI()))) {
            files = listing.filter(p -> p.getFileName().toString().endsWith(".properties")).sorted().toList();
        }
        return files.stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> verify(load(file))));
    }

    private void verify(Properties vector) {
        assertThat(REQUIRED_KEYS).allSatisfy(key -> assertThat(vector.getProperty(key)).as(key).isNotBlank());
        String payload = vector.getProperty("payload");

        VietQrPayload decoded = decoder.decode(payload);

        assertThat(decoded.bankBin()).isEqualTo(vector.getProperty("expected.bankBin"));
        assertThat(decoded.accountNumber()).isEqualTo(vector.getProperty("expected.accountNumber"));
        assertThat(decoded.dynamic()).isEqualTo(Boolean.parseBoolean(vector.getProperty("expected.dynamic")));
        // Absent amount or purpose means the payload must not carry one.
        String amount = vector.getProperty("expected.amount");
        assertThat(decoded.amount()).isEqualTo(amount == null ? null : Long.valueOf(amount));
        assertThat(decoded.purpose()).isEqualTo(vector.getProperty("expected.purpose"));
        if (vector.containsKey("expected.serviceCode")) {
            assertThat(decoded.serviceCode()).isEqualTo(ServiceCode.valueOf(vector.getProperty("expected.serviceCode")));
        }

        if (Boolean.parseBoolean(vector.getProperty("expected.reencodes"))) {
            VietQrRequest.Builder request = VietQrRequest.builder()
                    .bankBin(decoded.bankBin())
                    .accountNumber(decoded.accountNumber())
                    .serviceCode(decoded.serviceCode())
                    .purpose(decoded.purpose());
            if (decoded.amount() != null) {
                request.amount(decoded.amount());
            }
            assertThat(encoder.encode(request.build())).isEqualTo(payload);
        }
    }

    private static Properties load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
