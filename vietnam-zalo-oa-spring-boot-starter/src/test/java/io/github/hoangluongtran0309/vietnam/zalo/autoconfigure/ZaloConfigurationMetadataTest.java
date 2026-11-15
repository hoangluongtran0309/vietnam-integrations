package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloConfigurationMetadataTest {

    private static final String METADATA = "META-INF/spring-configuration-metadata.json";

    @Test
    void generatedMetadataContainsEverySupportedProperty() throws IOException {
        String metadata;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(METADATA)) {
            assertThat(input).as(METADATA).isNotNull();
            metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List.of(
                "vietnam.zalo-oa.app-id",
                "vietnam.zalo-oa.secret-key",
                "vietnam.zalo-oa.oa-id",
                "vietnam.zalo-oa.oa-secret-key",
                "vietnam.zalo-oa.token-store",
                "vietnam.zalo-oa.connect-timeout",
                "vietnam.zalo-oa.request-timeout",
                "vietnam.zalo-oa.jdbc.lock-timeout",
                "vietnam.zalo-oa.webhook.enabled",
                "vietnam.zalo-oa.webhook.path"
        ).forEach(property -> assertThat(metadata).contains("\"name\": \"" + property + "\""));
    }
}
