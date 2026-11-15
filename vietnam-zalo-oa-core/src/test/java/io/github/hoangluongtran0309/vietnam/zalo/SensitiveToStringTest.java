package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveToStringTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-11-16T03:00:00Z");

    @Test
    void tokenShowsOnlySecretLengthsAndExpiry() {
        String accessToken = "a".repeat(43);
        String refreshToken = "r".repeat(64);
        ZaloToken token = new ZaloToken(accessToken, refreshToken, EXPIRES_AT);

        assertThat(token.toString()).isEqualTo("ZaloToken[accessToken=<redacted:43>, "
                + "refreshToken=<redacted:64>, accessTokenExpiresAt=2026-11-16T03:00:00Z]");
        assertThat(token.toString()).doesNotContain(accessToken, refreshToken);
    }

    @Test
    void pkceShowsVerifierLengthButKeepsChallengeAndState() {
        String verifier = "code-verifier-secret";
        ZaloPkce pkce = new ZaloPkce(verifier, "allowed-challenge", "allowed-state");

        assertThat(pkce.toString())
                .isEqualTo("ZaloPkce[codeVerifier=<redacted:20>, codeChallenge=allowed-challenge, "
                        + "state=allowed-state]")
                .doesNotContain(verifier);
    }

    @Test
    void phoneTemplateRequestShowsMaskedPhoneAndTemplateKeysOnly() {
        ZbsPhoneTemplateRequest request = new ZbsPhoneTemplateRequest(
                "84912345123", "template-1", Map.of("customer", "private-template-value"), "tracking-1");

        assertThat(request.toString())
                .isEqualTo("ZbsPhoneTemplateRequest[phone=84******123, templateId=template-1, "
                        + "templateData=[customer], trackingId=tracking-1]")
                .doesNotContain("84912345123", "private-template-value");
    }

    @Test
    void messageResultShowsOnlyLastThreeRecipientCharacters() {
        ZaloMessageResult result = new ZaloMessageResult("message-1", "84912345123", EXPIRES_AT);

        assertThat(result.toString())
                .isEqualTo("ZaloMessageResult[messageId=message-1, recipient=84******123, "
                        + "sentAt=2026-11-16T03:00:00Z]")
                .doesNotContain("84912345123");
    }

    @Test
    void customToStringDoesNotChangeRecordEquality() {
        ZaloToken first = new ZaloToken("access", "refresh", EXPIRES_AT);
        ZaloToken second = new ZaloToken("access", "refresh", EXPIRES_AT);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }
}
