package io.github.hoangluongtran0309.vietnam.demo.capture;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureRedactorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CaptureRedactor redactor = new CaptureRedactor(mapper);

    @Test
    void redactsSecretFieldsAtEveryDepthIncludingArrays() {
        String input = "{\"access_token\":\"token-value\",\"items\":[{\"SECRET_KEY\":\"abc\"}],"
                + "\"nested\":{\"code_verifier\":\"verify\"}}";

        JsonNode result = redactor.redactJson(input);

        assertThat(result.path("access_token").asString()).isEqualTo("REDACTED_access_token_11");
        assertThat(result.at("/items/0/SECRET_KEY").asString()).isEqualTo("REDACTED_SECRET_KEY_3");
        assertThat(result.at("/nested/code_verifier").asString()).isEqualTo("REDACTED_code_verifier_6");
        assertThat(result.toString()).doesNotContain("token-value", "abc", "verify");
    }

    @Test
    void pseudonymizesIdentifiersAtEveryDepthAndOnlyContextualIds() {
        String input = "{\"sender\":{\"id\":\"User-123\"},\"items\":[{\"recipient\":{\"id\":\"User-123\"}}],"
                + "\"oa_id\":\"OA-ABC-9\",\"unrelated\":{\"id\":\"keep-secret\"}}";

        JsonNode result = redactor.redactJson(input);

        assertThat(result.at("/sender/id").asString()).isEqualTo(result.at("/items/0/recipient/id").asString())
                .hasSize("User-123".length()).matches("[A-Z][a-z]{3}-[0-9]{3}");
        assertThat(result.path("oa_id").asString()).hasSize("OA-ABC-9".length()).matches("[A-Z]{2}-[A-Z]{3}-[0-9]");
        assertThat(result.at("/unrelated/id").asString()).isEqualTo("REDACTED_11");
        assertThat(result.toString()).doesNotContain("User-123", "OA-ABC-9", "keep-secret");
    }

    @Test
    void pseudonymsAreStableOnlyWithinOneCaptureSession() {
        String first = redactor.pseudonymize("User-123");

        assertThat(redactor.pseudonymize("User-123")).isEqualTo(first);
        assertThat(new CaptureRedactor(mapper).pseudonymize("User-123")).isNotEqualTo(first);
    }

    @Test
    void preservesPseudonymLengthCharacterClassesAndJsonNumberType() {
        JsonNode result = redactor.redactJson("{\"user_id\":123456,\"tracking_id\":\"Ab-09_x\","
                + "\"message_id\":\"Đặng-123\"}");

        assertThat(result.path("user_id").isNumber()).isTrue();
        assertThat(result.path("user_id").toString()).hasSize(6).doesNotStartWith("0").matches("[0-9]{6}");
        assertThat(result.path("tracking_id").asString()).hasSize(7).matches("[A-Z][a-z]-[0-9]{2}_[a-z]");
        assertThat(result.path("message_id").asString()).hasSize(8).matches("[A-Z][a-z]{3}-[0-9]{3}")
                .doesNotContain("Đặng");
    }

    @Test
    void recordsCodePointAndUtf16LengthsForVietnameseTextWithEmoji() {
        JsonNode result = redactor.redactJson("{\"text\":\"Chào 👋\",\"message\":{\"text\":\"Đỏ 🚀\"}}");

        assertThat(result.path("text").asString()).isEqualTo("REDACTED_TEXT_6_7");
        assertThat(result.at("/message/text").asString()).isEqualTo("REDACTED_TEXT_4_5");
    }

    @Test
    void keepsAllowlistedValuesAndRootErrorMessage() {
        String input = "{\"error\":-1,\"message\":\"Invalid request\",\"event_name\":\"follow\","
                + "\"expires_in\":3600,\"status\":true,\"nothing\":null}";

        assertThat(redactor.redactJson(input)).isEqualTo(mapper.readTree(input));
    }

    @Test
    void preservesTemplateValueTypesAsMetadataAndCollectsDefaultFields() {
        JsonNode result = redactor.redactJson("{\"template_data\":{\"amount\":123456,\"name\":\"Lan\"},"
                + "\"unknown_string\":\"private\",\"unknown_number\":42}");

        assertThat(result.at("/template_data/amount").asString()).isEqualTo("REDACTED_number_6");
        assertThat(result.at("/template_data/name").asString()).isEqualTo("REDACTED_string_3");
        assertThat(result.path("unknown_string").asString()).isEqualTo("REDACTED_7");
        assertThat(result.path("unknown_number").intValue()).isEqualTo(42);
        assertThat(redactor.defaultFields()).containsExactlyInAnyOrder("unknown_number", "unknown_string");
        assertThat(result.toString()).doesNotContain("123456", "Lan", "private");
    }

    @Test
    void redactsFormAndQueryParametersWithoutSensitiveOriginals() {
        String input = "refresh_token=refresh-value&app_id=App-123&grant_type=refresh_token";

        String form = redactor.redactForm(input);
        String query = redactor.redactQuery(input);

        assertThat(form).isEqualTo(query).contains("refresh_token=REDACTED_refresh_token_13")
                .contains("grant_type=refresh_token")
                .doesNotContain("refresh-value", "App-123");
    }

    @Test
    void removesSecretHeadersAndRedactsSignatureWhileKeepingOperationalHeaders() {
        Map<String, List<String>> headers = Map.of(
                "Authorization", List.of("Bearer secret"),
                "secret_key", List.of("app-secret"),
                "X-ZEvent-Signature", List.of("mac=abcdef"),
                "Content-Type", List.of("application/json;charset=UTF-8"),
                "User-Agent", List.of("capture-test"));

        Map<String, List<String>> result = redactor.redactHeaders(headers);

        assertThat(result).doesNotContainKeys("Authorization", "secret_key")
                .containsEntry("X-ZEvent-Signature", List.of("mac=REDACTED_SIG_6"))
                .containsEntry("Content-Type", List.of("application/json;charset=UTF-8"))
                .containsEntry("User-Agent", List.of("capture-test"));
        assertThat(result.toString()).doesNotContain("Bearer secret", "app-secret", "abcdef");
    }
}
