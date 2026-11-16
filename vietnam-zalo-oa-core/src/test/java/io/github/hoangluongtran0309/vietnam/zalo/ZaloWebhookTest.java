package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloWebhookTest {

    private static final String APP = "360846524940903967";
    private static final String SECRET = "oa-secret";
    private final ZaloWebhookVerifier verifier = new ZaloWebhookVerifier(APP, SECRET);
    private final ZaloWebhookParser parser = new ZaloWebhookParser();

    @Test
    void verifiesAndParsesCapturedUserTextShape() {
        String body = """
                {"app_id":"360846524940903967","sender":{"id":"user-1"},
                 "recipient":{"id":"oa-1"},"event_name":"user_send_text",
                 "message":{"text":"message","msg_id":"msg-1"},"timestamp":"154390853474"}
                """.strip();
        String signature = signature(body, "154390853474");

        assertThat(verifier.verify(body, signature)).isTrue();
        assertThat(parser.parse(body)).isEqualTo(new UserTextMessageEvent(APP, "oa-1", "user-1", "oa-1",
                java.time.Instant.ofEpochMilli(154390853474L), "msg-1", "message"));
        assertThat(verifier.verify(body.replace("message", "tampered"), signature)).isFalse();
    }

    @Test
    void verifiesRawUtf8BodyWithVietnameseAndEmoji() {
        String body = event("new_event", ",\"text\":\"Xin chào Việt Nam 👋\"");
        String signature = signature(body, "1000");

        assertThat(verifier.verify(body.getBytes(StandardCharsets.UTF_8), signature)).isTrue();
        assertThat(verifier.verify(body, signature.substring(4))).isTrue();
        assertThat(verifier.verify(body, signature.toUpperCase())).isTrue();
    }

    @Test
    void rejectsChangedTimestampAndInvalidUtf8() {
        String body = event("follow", "");
        String signature = signature(body, "1000");

        assertThat(verifier.verify(body.replace("1000", "1001"), signature)).isFalse();
        assertThatThrownBy(() -> verifier.verify(new byte[] {(byte) 0xc3, (byte) 0x28}, signature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    void rejectsNullOrBlankVerifierCredentials() {
        assertThatThrownBy(() -> new ZaloWebhookVerifier(null, SECRET))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("appId");
        assertThatThrownBy(() -> new ZaloWebhookVerifier(" ", SECRET))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("appId");
        assertThatThrownBy(() -> new ZaloWebhookVerifier(APP, null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("oaSecretKey");
        assertThatThrownBy(() -> new ZaloWebhookVerifier(APP, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("oaSecretKey");
    }

    @Test
    void parsesCoreLifecycleEventsAndKeepsUnknownRawBody() {
        assertThat(parser.parse(event("follow", ""))).isInstanceOf(FollowEvent.class);
        assertThat(parser.parse(event("unfollow", ""))).isInstanceOf(UnfollowEvent.class);
        assertThat(parser.parse(event("user_received_message",
                ",\"message\":{\"msg_id\":\"m1\",\"tracking_id\":\"t1\",\"delivery_time\":\"1001\"}")))
                .isInstanceOf(MessageDeliveredEvent.class);
        assertThat(parser.parse(event("user_seen_message",
                ",\"message\":{\"msg_id\":\"m1\",\"seen_time\":\"1002\"}")))
                .isInstanceOf(MessageSeenEvent.class);
        String unknown = event("new_event", ",\"extra\":true");
        assertThat(parser.parse(unknown)).isEqualTo(new UnknownZaloWebhookEvent(
                "new_event", APP, "oa", "user", "oa", java.time.Instant.ofEpochMilli(1000), unknown));
    }

    @Test
    void rejectsMissingSignatureAndMalformedPayload() {
        assertThat(verifier.verify(event("follow", ""), null)).isFalse();
        assertThatThrownBy(() -> verifier.verify("{}", "mac=00"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timestamp");
        assertThatThrownBy(() -> parser.parse("not-json")).isInstanceOf(IllegalArgumentException.class);
    }

    private static String event(String name, String extra) {
        return "{\"app_id\":\"" + APP + "\",\"sender\":{\"id\":\"user\"},"
                + "\"recipient\":{\"id\":\"oa\"},\"event_name\":\"" + name
                + "\",\"timestamp\":\"1000\"" + extra + "}";
    }

    private static String signature(String body, String timestamp) {
        return "mac=" + ZaloWebhookVerifier.sha256Hex(APP + body + timestamp + SECRET);
    }
}
