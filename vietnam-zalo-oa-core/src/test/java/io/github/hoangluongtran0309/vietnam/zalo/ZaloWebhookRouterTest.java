package io.github.hoangluongtran0309.vietnam.zalo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloWebhookRouterTest {

    private static final String APP = "app";
    private static final String OA_1_SECRET = "webhook-secret-1";
    private static final String OA_2_SECRET = "webhook-secret-2";

    private final ZaloWebhookRouter router = new ZaloWebhookRouter(registry());

    @Test
    void routesSecondAccountAndVerifiesWithItsSecret() {
        String body = fixture("user-text.json");

        ZaloWebhookEvent event = router.route(bytes(body), signature(body, APP, OA_2_SECRET));

        assertThat(event).isInstanceOf(UserTextMessageEvent.class);
        assertThat(event.oaId()).isEqualTo("oa-2");
        assertThat(event.recipientId()).isEqualTo("oa-1");
    }

    @Test
    void rejectsSignatureFromOtherAccount() {
        String body = fixture("user-text.json");

        assertThatThrownBy(() -> router.route(bytes(body), signature(body, APP, OA_1_SECRET)))
                .isInstanceOf(ZaloWebhookRejectedException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsUnknownAccountAndMismatchedApplication() {
        String knownBody = fixture("user-text.json");
        String unknownAccount = knownBody.replace("\"oa_id\":\"oa-2\"", "\"oa_id\":\"oa-unknown\"");
        String otherApplication = knownBody.replace("\"app_id\":\"app\"", "\"app_id\":\"other-app\"");

        assertThatThrownBy(() -> router.route(
                bytes(unknownAccount), signature(unknownAccount, APP, OA_2_SECRET)))
                .isInstanceOf(ZaloWebhookRejectedException.class)
                .hasMessageContaining("not configured");
        assertThatThrownBy(() -> router.route(
                bytes(otherApplication), signature(otherApplication, "other-app", OA_2_SECRET)))
                .isInstanceOf(ZaloWebhookRejectedException.class)
                .hasMessageContaining("app_id");
    }

    @Test
    void routesAllSupportedEventsAndUnknownEventFromResourceFixtures() {
        Map<String, Class<? extends ZaloWebhookEvent>> fixtures = Map.of(
                "follow.json", FollowEvent.class,
                "unfollow.json", UnfollowEvent.class,
                "delivered.json", MessageDeliveredEvent.class,
                "seen.json", MessageSeenEvent.class,
                "unknown.json", UnknownZaloWebhookEvent.class);

        fixtures.forEach((name, eventType) -> {
            String body = fixture(name);
            ZaloWebhookEvent event = router.route(bytes(body), signature(body, APP, OA_2_SECRET));
            assertThat(event).isInstanceOf(eventType);
            assertThat(event.oaId()).isEqualTo("oa-2");
        });
    }

    @Test
    void rejectsAmbiguousFallbackAccount() {
        String body = fixture("follow.json")
                .replace("\"sender\":{\"id\":\"user-2\"}", "\"sender\":{\"id\":\"oa-1\"}");

        assertThatThrownBy(() -> router.route(bytes(body), signature(body, APP, OA_2_SECRET)))
                .isInstanceOf(ZaloWebhookRejectedException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void reportsMalformedBodyAsClientInputError() {
        String invalidTimestamp = fixture("follow.json").replace("\"1000\"", "\"not-a-number\"");

        assertThatThrownBy(() -> router.route(new byte[] {(byte) 0xc3, (byte) 0x28}, "mac=bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8");
        assertThatThrownBy(() -> router.route(bytes("not-json"), "mac=bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");
        assertThatThrownBy(() -> router.route(
                bytes(invalidTimestamp), signature(invalidTimestamp, APP, OA_2_SECRET)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void rejectsRegistryAccountWithoutWebhookSecretAtConstructionTime() {
        ZaloOaRegistry registry = ZaloOaRegistry.builder(List.of(
                        new ZaloAccountConfig("missing-secret", APP, "app-secret", "oa", null)),
                new InMemoryZaloTokenStore()).build();

        assertThatThrownBy(() -> new ZaloWebhookRouter(registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing-secret");
    }

    private static ZaloOaRegistry registry() {
        return ZaloOaRegistry.builder(List.of(
                        new ZaloAccountConfig("first", APP, "app-secret", "oa-1", OA_1_SECRET),
                        new ZaloAccountConfig("second", APP, "app-secret", "oa-2", OA_2_SECRET)),
                new InMemoryZaloTokenStore()).build();
    }

    private static byte[] bytes(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private static String signature(String body, String appId, String secret) {
        return "mac=" + ZaloWebhookVerifier.sha256Hex(appId + body + "1000" + secret);
    }

    private static String fixture(String name) {
        String path = "zalo/webhook/" + name;
        try (InputStream stream = ZaloWebhookRouterTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing test fixture " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read test fixture " + path, e);
        }
    }
}
