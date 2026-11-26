package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EInvoiceConfigurationMetadataTest {

    @Test
    void generatedMetadataContainsEverySupportedProperty() throws IOException {
        String path = "META-INF/spring-configuration-metadata.json";
        String metadata;
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List.of("provider", "xml.max-input-size", "xml.max-elements", "validation.amount-tolerance",
                "signature.enabled", "signature.trust-anchors", "signature.intermediate-certificates")
                .forEach(property -> assertThat(metadata).contains("\"name\": \"vietnam.einvoice." + property + "\""));
    }
}
