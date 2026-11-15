package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.UserTextMessageEvent;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookParser;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloWebhookAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner(
            WebApplicationContextRunner.withMockServletContext(AnnotationConfigServletWebApplicationContext::new))
            .withConfiguration(AutoConfigurations.of(ZaloOaAutoConfiguration.class, ZaloWebhookAutoConfiguration.class))
            .withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=app-secret",
                    "vietnam.zalo-oa.oa-id=oa");

    @Test
    void webhookIsOptIn() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ZaloWebhookController.class));
    }

    @Test
    void registersControllerInServletApplicationWhenEnabled() {
        runner.withPropertyValues("vietnam.zalo-oa.webhook.enabled=true",
                        "vietnam.zalo-oa.oa-secret-key=oa-secret")
                .run(ctx -> assertThat(ctx).hasSingleBean(ZaloWebhookController.class));
    }

    @Test
    void failsFastWhenWebhookSecretIsMissing() {
        runner.withPropertyValues("vietnam.zalo-oa.webhook.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("vietnam.zalo-oa.oa-secret-key")
                        .hasMessageNotContaining("app-secret"));
    }

    @Test
    void controllerVerifiesParsesAndPublishesTypedEvent() {
        String body = """
                {"app_id":"app","sender":{"id":"user"},"recipient":{"id":"oa"},
                 "event_name":"user_send_text","message":{"msg_id":"m1","text":"hello"},"timestamp":"1000"}
                """.strip();
        AtomicReference<Object> published = new AtomicReference<>();
        ZaloWebhookController controller = new ZaloWebhookController(
                new ZaloWebhookVerifier("app", "oa-secret"), new ZaloWebhookParser(), published::set);

        assertThat(controller.receive(body, signature(body)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(published.get()).isInstanceOf(UserTextMessageEvent.class);
        assertThat(controller.receive(body, "mac=bad").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.receive("{}", "mac=bad").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listenerFailurePropagatesForWebhookRetry() {
        String body = """
                {"app_id":"app","sender":{"id":"user"},"recipient":{"id":"oa"},
                 "event_name":"user_send_text","message":{"msg_id":"m1","text":"hello"},"timestamp":"1000"}
                """.strip();
        ZaloWebhookController controller = new ZaloWebhookController(new ZaloWebhookVerifier("app", "oa-secret"),
                new ZaloWebhookParser(), event -> { throw new IllegalStateException("listener failed"); });

        assertThatThrownBy(() -> controller.receive(body, signature(body)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("listener failed");
    }

    private static String signature(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("app" + body + "1000" + "oa-secret").getBytes(StandardCharsets.UTF_8));
            return "mac=" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
