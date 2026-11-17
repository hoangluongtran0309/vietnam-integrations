package io.github.hoangluongtran0309.vietnam.zalo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
                .willReturn(okJson(fixture("oauth-code-token.json"))));
        HttpZaloOAuthClient client = oauth();

        ZaloToken token = client.exchangeAuthorizationCode("code+value", "verifier");

        assertThat(token).isEqualTo(new ZaloToken("access", "refresh", NOW.plusSeconds(90_000)));
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void acceptsNumericOAuthExpiry() {
        server.stubFor(post("/oauth").willReturn(okJson(fixture("oauth-code-token-numeric-expiry.json"))));

        ZaloToken token = oauth().exchangeAuthorizationCode("code", "verifier");

        assertThat(token.accessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(90_000));
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void refreshesOnceAndReportsSanitizedApiError() {
        server.stubFor(post("/oauth").willReturn(okJson("{" +
                "\"error\":-14014,\"message\":\"Refresh token expired\"}")));

        assertThatThrownBy(() -> oauth().refresh("super-secret-refresh"))
                .isInstanceOf(ZaloReauthorizationRequiredException.class)
                .hasMessageContaining("-14014")
                .hasMessageContaining("authorization flow")
                .hasMessageNotContaining("super-secret-refresh");
        server.verify(1, postRequestedFor(urlEqualTo("/oauth")));
    }

    @Test
    void doesNotMapReauthorizationCodeDuringAuthorizationCodeExchange() {
        server.stubFor(post("/oauth").willReturn(okJson("{" +
                "\"error\":-14014,\"message\":\"Authorization code rejected\"}")));

        assertThatThrownBy(() -> oauth().exchangeAuthorizationCode("code", "verifier"))
                .isExactlyInstanceOf(ZaloApiException.class)
                .isNotInstanceOf(ZaloReauthorizationRequiredException.class);
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
    void reportsHttpStatusForValidJsonServerError() {
        server.stubFor(post("/oauth").willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":-1,\"message\":\"temporarily unavailable\"}")));

        assertThatThrownBy(() -> oauth().refresh("refresh"))
                .isInstanceOfSatisfying(ZaloApiException.class, exception -> {
                    assertThat(exception.httpStatus()).isEqualTo(503);
                    assertThat(exception.operation()).isEqualTo("Zalo OAuth token refresh");
                })
                .isNotInstanceOf(ZaloReauthorizationRequiredException.class);
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
                .willReturn(okJson(fixture("oa-message-success.json"))));

        ZaloMessageResult result = new HttpZaloOaClient(manager(), HttpClient.newHttpClient(),
                uri("/oa-message"), Duration.ofSeconds(2))
                .sendConsultationText(new ZaloConsultationTextRequest("user-1", "Xin chào", "msg-0"));

        assertThat(result.messageId()).isEqualTo("msg-1");
        assertThat(result.recipient()).isEqualTo("user-1");
        server.verify(1, postRequestedFor(urlEqualTo("/oa-message")));
    }

    @Test
    void escapesUnicodeAndJsonControlCharactersInConsultationText() {
        String text = "Tiếng Việt 😀, dấu \"quote\", gạch \\ và\nxuống dòng";
        String expectedJson = "{\"recipient\":{\"user_id\":\"user-1\"},\"message\":{\"text\":"
                + "\"Tiếng Việt 😀, dấu \\\"quote\\\", gạch \\\\ và\\nxuống dòng\"}}";
        server.stubFor(post("/oa-message")
                .withRequestBody(equalToJson(expectedJson))
                .willReturn(okJson("""
                        {"data":{"message_id":"msg-1","sent_time":"1794528000000"},"error":0}
                        """)));

        new HttpZaloOaClient(manager(), HttpClient.newHttpClient(), uri("/oa-message"), Duration.ofSeconds(2))
                .sendConsultationText(new ZaloConsultationTextRequest("user-1", text));

        String rawBody = server.getAllServeEvents().get(0).getRequest().getBodyAsString();
        assertThat(rawBody)
                .contains("\\\"quote\\\"")
                .contains("gạch \\\\ và\\nxuống dòng");
        server.verify(1, postRequestedFor(urlEqualTo("/oa-message")));
    }

    @Test
    void reportsOaApiErrorWithOperationWithoutRetry() {
        server.stubFor(post("/oa-message").willReturn(okJson("""
                {"error":-201,"message":"OA rejected the message"}
                """)));

        assertThatThrownBy(() -> new HttpZaloOaClient(manager(), HttpClient.newHttpClient(),
                uri("/oa-message"), Duration.ofSeconds(2))
                .sendConsultationText(new ZaloConsultationTextRequest("user-1", "Hello")))
                .isInstanceOfSatisfying(ZaloApiException.class, exception -> {
                    assertThat(exception.apiErrorCode()).isEqualTo(-201);
                    assertThat(exception.operation()).isEqualTo("send OA consultation text");
                });
        server.verify(1, postRequestedFor(urlEqualTo("/oa-message")));
    }

    @Test
    void sendsZbsTemplateByPhone() {
        server.stubFor(post("/zbs")
                .withHeader("access_token", equalTo("access"))
                .withRequestBody(equalToJson("""
                        {"phone":"84912345678","template_id":"template-1",
                         "template_data":{"amount":100,"name":"Nguyễn Văn A","decimal":0.0000001},
                         "tracking_id":"order-123"}
                        """))
                .withRequestBody(containing("\"decimal\":0.0000001"))
                .willReturn(okJson(fixture("zbs-message-success.json"))));

        ZaloMessageResult result = new HttpZaloZbsClient(manager(), HttpClient.newHttpClient(), uri("/zbs"),
                Duration.ofSeconds(2)).sendTemplateByPhone(new ZbsPhoneTemplateRequest(
                "(+84) 912.345-678", "template-1",
                Map.of("name", "Nguyễn Văn A", "amount", 100, "decimal", new BigDecimal("1E-7")),
                "order-123"));

        assertThat(result.messageId()).isEqualTo("zbs-1");
        assertThat(result.recipient()).isEqualTo("84912345678");
        server.verify(1, postRequestedFor(urlEqualTo("/zbs")));
    }

    @Test
    void reportsZbsApiErrorWithOperationWithoutRetry() {
        server.stubFor(post("/zbs").willReturn(okJson("""
                {"error":-202,"message":"ZBS rejected the message"}
                """)));

        assertThatThrownBy(() -> new HttpZaloZbsClient(manager(), HttpClient.newHttpClient(),
                uri("/zbs"), Duration.ofSeconds(2))
                .sendTemplateByPhone(new ZbsPhoneTemplateRequest(
                        "84912345678", "template", Map.of(), "tracking")))
                .isInstanceOfSatisfying(ZaloApiException.class, exception -> {
                    assertThat(exception.apiErrorCode()).isEqualTo(-202);
                    assertThat(exception.operation()).isEqualTo("send ZBS template by phone");
                });
        server.verify(1, postRequestedFor(urlEqualTo("/zbs")));
    }

    @Test
    void acceptsMessageLengthAndTrackingIdBoundaries() {
        assertThatCode(() -> new ZaloConsultationTextRequest("user", "x".repeat(2_000)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new ZaloConsultationTextRequest("user", "x".repeat(2_001)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2000");
        assertThatCode(() -> new ZbsPhoneTemplateRequest(
                "84912345678", "template", Map.of(), "t".repeat(48)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "84912345678", "template", Map.of(), "t".repeat(49)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("trackingId");
    }

    @Test
    void normalizesSupportedPhoneStructuresWithoutCheckingCarrierPrefixes() {
        assertThat(new ZbsPhoneTemplateRequest(
                "0912 345 678", "template", Map.of(), "track").phone()).isEqualTo("84912345678");
        assertThat(new ZbsPhoneTemplateRequest(
                "(84) 912.345-678", "template", Map.of(), "track").phone()).isEqualTo("84912345678");
        assertThat(new ZbsPhoneTemplateRequest(
                "+84-912-345-678", "template", Map.of(), "track").phone()).isEqualTo("84912345678");
        assertThat(new ZbsPhoneTemplateRequest(
                "0123456789", "template", Map.of(), "track").phone()).isEqualTo("84123456789");

        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "84 912 345 67", "template", Map.of(), "track"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("9 digits");
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "+83 912 345 678", "template", Map.of(), "track"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("country code 84");
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "0912/345/678", "template", Map.of(), "track"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("phone");
    }

    @Test
    void acceptsOnlyExplicitTemplateDataScalarTypes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("string", "value");
        values.put("boolean", true);
        values.put("integer", 1);
        values.put("long", 2L);
        values.put("short", (short) 3);
        values.put("byte", (byte) 4);
        values.put("bigInteger", new BigInteger("12345678901234567890"));
        values.put("bigDecimal", new BigDecimal("123.45"));

        assertThatCode(() -> new ZbsPhoneTemplateRequest(
                "84912345678", "template", values, "track"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsupportedTemplateDataTypesAndNamesTheKey() {
        assertInvalidTemplateValue("double", 1.5d);
        assertInvalidTemplateValue("float", 1.5f);
        assertInvalidTemplateValue("date", LocalDate.of(2026, 11, 15));
        assertInvalidTemplateValue("nullValue", null);

        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "84912345678", "template", Map.of(" ", "value"), "track"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
    }

    @Test
    void preservesInterruptStatusWhenHttpSendIsInterrupted() {
        try {
            HttpZaloOAuthClient client = new HttpZaloOAuthClient(
                    "123", "secret", new InterruptingHttpClient(), uri("/oauth"),
                    Duration.ofSeconds(2), Clock.fixed(NOW, ZoneOffset.UTC));

            assertThatThrownBy(() -> client.refresh("refresh"))
                    .isInstanceOfSatisfying(ZaloApiException.class, exception ->
                            assertThat(exception.operation()).isEqualTo("Zalo OAuth token refresh"))
                    .hasMessageContaining("request interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
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

    private static String fixture(String name) {
        try (var input = Objects.requireNonNull(
                HttpZaloClientsTest.class.getResourceAsStream("/zalo/http/" + name))) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void assertInvalidTemplateValue(String key, Object value) {
        Map<String, Object> templateData = new LinkedHashMap<>();
        templateData.put(key, value);
        assertThatThrownBy(() -> new ZbsPhoneTemplateRequest(
                "84912345678", "template", templateData, "track"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(key);
    }

    private static final class InterruptingHttpClient extends HttpClient {

        private final HttpClient delegate = HttpClient.newHttpClient();

        @Override public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
        @Override public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
        @Override public Redirect followRedirects() { return delegate.followRedirects(); }
        @Override public Optional<ProxySelector> proxy() { return delegate.proxy(); }
        @Override public SSLContext sslContext() { return delegate.sslContext(); }
        @Override public SSLParameters sslParameters() { return delegate.sslParameters(); }
        @Override public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
        @Override public Version version() { return delegate.version(); }
        @Override public Optional<Executor> executor() { return delegate.executor(); }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            throw new InterruptedException("simulated interruption");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("not used");
        }
    }
}
