package io.github.hoangluongtran0309.vietnam.zalo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpZaloClientsTest {

    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");
    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void exchangesAuthorizationCodeUsingExactOAuthContract() {
        server.stubFor(post("/oauth")
                .withHeader("secret_key", equalTo("app-secret"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("code=code%2Bvalue"))
                .withRequestBody(containing("app_id=123"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code_verifier=verifier"))
                .willReturn(okJson("""
                        {"access_token":"access","refresh_token":"refresh","expires_in":"90000"}
                        """)));
        HttpZaloOAuthClient client = oauth();

        ZaloToken token = client.exchangeAuthorizationCode("code+value", "verifier");

        assertThat(token).isEqualTo(new ZaloToken("access", "refresh", NOW.plusSeconds(90_000)));
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void refreshesOnceAndReportsSanitizedApiError() {
        server.stubFor(post("/oauth").willReturn(okJson("{" +
                "\"error\":-14014,\"message\":\"Refresh token expired\"}")));

        assertThatThrownBy(() -> oauth().refresh("super-secret-refresh"))
                .isInstanceOf(ZaloApiException.class)
                .hasMessageContaining("-14014")
                .hasMessageNotContaining("super-secret-refresh");
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void rejectsMalformedJsonWithoutExposingBody() {
        server.stubFor(post("/oauth").willReturn(aResponse().withStatus(502).withBody("proxy-secret-body")));

        assertThatThrownBy(() -> oauth().refresh("refresh"))
                .isInstanceOf(ZaloApiException.class)
                .hasMessageContaining("invalid JSON")
                .hasMessageNotContaining("proxy-secret-body");
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void timesOutWithoutRetryingAmbiguousRequest() {
        server.stubFor(post("/slow").willReturn(okJson("{}" ).withFixedDelay(250)));
        HttpZaloOAuthClient client = new HttpZaloOAuthClient("123", "secret", HttpClient.newHttpClient(),
                uri("/slow"), Duration.ofMillis(50), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> client.refresh("refresh"))
                .isInstanceOf(ZaloApiException.class)
                .hasMessageContaining("was not retried");
        server.verify(1, postRequestedFor(urlEqualTo("/slow")));
    }

    @Test
    void sendsConsultationTextWithoutRetry() {
        server.stubFor(post("/oa-message")
                .withHeader("access_token", equalTo("access"))
                .withRequestBody(equalToJson("""
                        {"recipient":{"user_id":"user-1"},"message":{"text":"Xin chào","quote_message_id":"msg-0"}}
                        """))
                .willReturn(okJson("""
                        {"data":{"message_id":"msg-1","user_id":"user-1","sent_time":"1794528000000"},
                         "error":0,"message":"Success"}
                        """)));

        ZaloMessageResult result = new HttpZaloOaClient(manager(), HttpClient.newHttpClient(),
                uri("/oa-message"), Duration.ofSeconds(2))
                .sendConsultationText(new ZaloConsultationTextRequest("user-1", "Xin chào", "msg-0"));

        assertThat(result.messageId()).isEqualTo("msg-1");
        assertThat(result.recipient()).isEqualTo("user-1");
        server.verify(1, postRequestedFor(urlEqualTo("/oa-message")));
    }

    @Test
    void sendsZbsTemplateByPhone() {
        server.stubFor(post("/zbs")
                .withHeader("access_token", equalTo("access"))
                .withRequestBody(equalToJson("""
                        {"phone":"84912345678","template_id":"template-1",
                         "template_data":{"amount":100,"name":"Nguyễn Văn A"},"tracking_id":"order-123"}
                        """))
                .willReturn(okJson("""
                        {"data":{"msg_id":"zbs-1","sent_time":"1794528000000"},"error":0,"message":"Success"}
                        """)));

        ZaloMessageResult result = new HttpZaloZbsClient(manager(), HttpClient.newHttpClient(), uri("/zbs"),
                Duration.ofSeconds(2)).sendTemplateByPhone(new ZbsPhoneTemplateRequest(
                "84912345678", "template-1", Map.of("name", "Nguyễn Văn A", "amount", 100), "order-123"));

        assertThat(result.messageId()).isEqualTo("zbs-1");
        assertThat(result.recipient()).isEqualTo("84912345678");
        server.verify(1, postRequestedFor(urlEqualTo("/zbs")));
    }

    @Test
    void validatesMessageInputsBeforeCallingNetwork() {
        assertThatThrownBy(() -> new ZaloConsultationTextRequest("user", "x".repeat(2_001)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2000");
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest("0912345678", "template", Map.of(), "track"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("normalized");
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest("84912345678", "template", Map.of(), "bad tracking!"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("trackingId");
    }

    private HttpZaloOAuthClient oauth() {
        return new HttpZaloOAuthClient("123", "app-secret", HttpClient.newHttpClient(), uri("/oauth"),
                Duration.ofSeconds(2), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ZaloTokenManager manager() {
        InMemoryZaloTokenStore store = new InMemoryZaloTokenStore();
        store.save("oa", new ZaloToken("access", "refresh", NOW.plus(Duration.ofHours(1))));
        return new ZaloTokenManager("oa", store, new ZaloOAuthClient() {
            @Override public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
                throw new AssertionError("not expected");
            }
            @Override public ZaloToken refresh(String refreshToken) { throw new AssertionError("not expected"); }
        }, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private URI uri(String path) {
        return URI.create(server.baseUrl() + path);
    }
}
