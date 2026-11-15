package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.UserTextMessageEvent;
import io.github.hoangluongtran0309.vietnam.zalo.UnknownZaloWebhookEvent;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookParser;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ZaloWebhookAutoConfigurationTest {

    private static final String WEBHOOK_PATH = "/webhooks/zalo";

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
    void controllerVerifiesRawBodyAndPublishesTypedEvent() throws Exception {
        String body = userTextBody();
        AtomicReference<Object> published = new AtomicReference<>();
        MockMvc mockMvc = mockMvc(published::set);

        mockMvc.perform(signedRequest(body).contentType("application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(emptyBody());

        assertThat(published.get()).isInstanceOf(UserTextMessageEvent.class);
    }

    @Test
    void controllerAcceptsUnknownEventWithAnyOrMissingContentType() throws Exception {
        String body = eventBody("future_event", "1000");
        AtomicReference<Object> published = new AtomicReference<>();
        MockMvc mockMvc = mockMvc(published::set);

        mockMvc.perform(signedRequest(body).contentType("application/octet-stream"))
                .andExpect(status().isOk());
        mockMvc.perform(signedRequest(body))
                .andExpect(status().isOk());

        assertThat(published.get()).isInstanceOf(UnknownZaloWebhookEvent.class);
    }

    @Test
    void controllerReturnsEmpty401ForMissingOrInvalidSignature() throws Exception {
        String body = userTextBody();
        MockMvc mockMvc = mockMvc(event -> { });

        mockMvc.perform(post(WEBHOOK_PATH).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(emptyBody());
        mockMvc.perform(post(WEBHOOK_PATH).content(body).header("X-ZEvent-Signature", "mac=bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(emptyBody());
    }

    @Test
    void controllerReturnsEmpty400ForMalformedJsonOrTimestamp() throws Exception {
        MockMvc mockMvc = mockMvc(event -> { });
        String nonNumericTimestamp = eventBody("follow", "not-a-number");

        mockMvc.perform(post(WEBHOOK_PATH).content("sensitive-not-json")
                        .header("X-ZEvent-Signature", "mac=bad"))
                .andExpect(status().isBadRequest())
                .andExpect(emptyBody());
        mockMvc.perform(post(WEBHOOK_PATH).content("{}")
                        .header("X-ZEvent-Signature", "mac=bad"))
                .andExpect(status().isBadRequest())
                .andExpect(emptyBody());
        mockMvc.perform(signedRequest(nonNumericTimestamp, "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(emptyBody());
    }

    @Test
    void controllerReturnsEmpty400ForInvalidUtf8() throws Exception {
        MockMvc mockMvc = mockMvc(event -> { });

        mockMvc.perform(post(WEBHOOK_PATH).content(new byte[] {(byte) 0xc3, (byte) 0x28})
                        .header("X-ZEvent-Signature", "mac=bad"))
                .andExpect(status().isBadRequest())
                .andExpect(emptyBody());
    }

    @Test
    void controllerCatchesListenerFailureAndReturnsEmpty500() throws Exception {
        String body = userTextBody();
        MockMvc mockMvc = mockMvc(event -> {
            throw new IllegalStateException("listener failed with sensitive details");
        });

        mockMvc.perform(signedRequest(body))
                .andExpect(status().isInternalServerError())
                .andExpect(emptyBody());
    }

    private static String signature(String body) {
        return signature(body, "1000");
    }

    private static String signature(String body, String timestamp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("app" + body + timestamp + "oa-secret").getBytes(StandardCharsets.UTF_8));
            return "mac=" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MockMvc mockMvc(ApplicationEventPublisher publisher) {
        ZaloWebhookController controller = new ZaloWebhookController(new ZaloWebhookVerifier("app", "oa-secret"),
                new ZaloWebhookParser(), publisher);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static ResultMatcher emptyBody() {
        return result -> assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
    }

    private static MockHttpServletRequestBuilder signedRequest(String body) {
        return signedRequest(body, "1000");
    }

    private static MockHttpServletRequestBuilder signedRequest(String body, String timestamp) {
        return post(WEBHOOK_PATH).content(body).header("X-ZEvent-Signature", signature(body, timestamp));
    }

    private static String userTextBody() {
        return """
                {"app_id":"app","sender":{"id":"user"},"recipient":{"id":"oa"},
                 "event_name":"user_send_text","message":{"msg_id":"m1","text":"Xin chào 👋"},"timestamp":"1000"}
                """.strip();
    }

    private static String eventBody(String eventName, String timestamp) {
        return "{\"app_id\":\"app\",\"sender\":{\"id\":\"user\"},\"recipient\":{\"id\":\"oa\"},"
                + "\"event_name\":\"" + eventName + "\",\"timestamp\":\"" + timestamp + "\"}";
    }
}
