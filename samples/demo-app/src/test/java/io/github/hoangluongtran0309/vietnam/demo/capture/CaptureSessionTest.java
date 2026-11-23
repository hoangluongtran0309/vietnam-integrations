package io.github.hoangluongtran0309.vietnam.demo.capture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureSessionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesNullForNullOrBlankAccountIdentifiers(@TempDir Path base) throws Exception {
        CaptureSession session = new CaptureSession(base.toString(), mapper,
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC),
                List.of(
                        new CaptureSession.Account("null-oa", null, " \t"),
                        new CaptureSession.Account("null-app", " \t", null)));

        JsonNode aliases = mapper.readTree(session.directory().resolve("session.json").toFile()).path("aliases");

        assertThat(aliases.path(0).path("oaId").isNull()).isTrue();
        assertThat(aliases.path(0).path("appId").isNull()).isTrue();
        assertThat(aliases.path(1).path("oaId").isNull()).isTrue();
        assertThat(aliases.path(1).path("appId").isNull()).isTrue();
    }
}
