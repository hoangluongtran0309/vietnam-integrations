package io.github.hoangluongtran0309.vietnam.demo.capture;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import io.github.hoangluongtran0309.vietnam.demo.DemoApplication;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.autoconfigure.ZaloOaProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "debug=false",
                "vietnam.zalo-oa.enabled=true",
                "vietnam.zalo-oa.app-id=app",
                "vietnam.zalo-oa.secret-key=test-app-secret",
                "vietnam.zalo-oa.default-account=cskh",
                "vietnam.zalo-oa.accounts.cskh.oa-id=oa-1",
                "vietnam.zalo-oa.accounts.cskh.oa-secret-key=test-oa-secret-1",
                "vietnam.zalo-oa.accounts.marketing.oa-id=oa-2",
                "vietnam.zalo-oa.accounts.marketing.oa-secret-key=test-oa-secret-2",
                "vietnam.zalo-oa.token-store=jdbc",
                "vietnam.zalo-oa.request-timeout=2s",
                "vietnam.zalo-oa.jdbc.lock-timeout=3s",
                "vietnam.zalo-oa.webhook.enabled=true"
        })
@Import(ZaloCaptureEndToEndTest.FakeZaloConfiguration.class)
class ZaloCaptureEndToEndTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-11-17T00:00:00Z");
    private static final PostgreSQLContainer POSTGRES = startPostgres();
    private static final WireMockServer ZALO = startFakeZalo();

    @TempDir
    static Path captureBase;

    @DynamicPropertySource
    static void fakeServices(DynamicPropertyRegistry properties) {
        properties.add("demo.zalo.capture-dir", captureBase::toString);
        properties.add("demo.zalo.jdbc-url", POSTGRES::getJdbcUrl);
        properties.add("demo.zalo.jdbc-user", POSTGRES::getUsername);
        properties.add("demo.zalo.jdbc-password", POSTGRES::getPassword);
        properties.add("demo.zalo.authorization-uri", () -> ZALO.baseUrl() + "/authorize");
        properties.add("demo.zalo.redirect-uri", () -> "http://demo.invalid/zalo/oauth/callback");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CaptureSession captureSession;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void capturesTheCompleteFakeZaloFlowWithoutOriginalSensitiveValues() throws Exception {
        stubFakeZalo();
        HttpClient browser = browser();

        HttpResponse<String> start = browser.send(get("/zalo/oauth/start?account=cskh"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(start.statusCode()).isEqualTo(302);
        URI authorization = URI.create(start.headers().firstValue("Location").orElseThrow());
        String state = queryParameter(authorization, "state");
        String challenge = queryParameter(authorization, "code_challenge");

        HttpResponse<String> callback = browser.send(get("/zalo/oauth/callback?code=" + encode("code+value")
                        + "&oa_id=oa-1&state=" + encode(state)), HttpResponse.BodyHandlers.ofString());
        assertThat(callback.statusCode()).isEqualTo(200);
        String verifier = oauthCodeVerifier();

        clock.advance(Duration.ofHours(26));
        HttpResponse<String> consultation = browser.send(appPost(
                        "/zalo/consultation?account=cskh&userId=user-1&text=" + encode("Xin chào"), ""),
                HttpResponse.BodyHandlers.ofString());
        assertThat(consultation.statusCode()).isEqualTo(200);

        String zbsRequest = new Gson().toJson(Map.of(
                "phone", "(+84) 912.345-678",
                "templateId", "template-1",
                "templateData", Map.of("amount", 100, "name", "Nguyễn Văn A"),
                "trackingId", "order-123"));
        HttpResponse<String> zbs = browser.send(appPost("/zalo/zbs?account=cskh", zbsRequest),
                HttpResponse.BodyHandlers.ofString());
        assertThat(zbs.statusCode()).isEqualTo(200);

        String webhookBody = fixture("/zalo/webhook/user-text.json");
        HttpResponse<String> webhook = browser.send(HttpRequest.newBuilder(appUri("/webhooks/zalo"))
                        .header("Content-Type", "application/json")
                        .header("X-ZEvent-Signature", signature(webhookBody, "test-oa-secret-2"))
                        .POST(HttpRequest.BodyPublishers.ofString(webhookBody)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(webhook.statusCode()).isEqualTo(200);

        captureSession.close();
        List<Path> files;
        try (var stream = Files.walk(captureBase)) {
            files = stream.filter(Files::isRegularFile).sorted().toList();
        }
        assertThat(files).extracting(path -> path.getFileName().toString())
                .anyMatch(name -> name.equals("session.json"))
                .anyMatch(name -> name.contains("-http-POST-oauth"))
                .anyMatch(name -> name.contains("-http-POST-oa-message"))
                .anyMatch(name -> name.contains("-http-POST-zbs"))
                .anyMatch(name -> name.contains("-webhook-user-send-text"));
        assertThat(files.stream().filter(path -> path.getFileName().toString().contains("-http-POST-oauth")))
                .hasSize(2);
        files.forEach(path -> assertThat(mapper.readTree(path.toFile())).isNotNull());

        String allCaptures = String.join("\n", files.stream().map(ZaloCaptureEndToEndTest::read).toList());
        assertThat(allCaptures).doesNotContain(
                "\"access\"", "access_token=access", "\"refresh\"", "refresh_token=refresh",
                "new-access-1", "new-refresh-1", "code+value", verifier, challenge,
                "test-app-secret", "test-oa-secret-1", "test-oa-secret-2", "84912345678", "user-1", "user-2",
                "Xin chào", "oa-1", "oa-2", "\"app\"", "app_id=app");
    }

    @AfterAll
    static void stopFakeServices() {
        ZALO.stop();
        POSTGRES.stop();
    }

    private HttpClient browser() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(appUri(path)).GET().build();
    }

    private HttpRequest appPost(String path, String body) {
        return HttpRequest.newBuilder(appUri(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private URI appUri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static void stubFakeZalo() {
        ZALO.resetAll();
        ZALO.stubFor(post("/oauth").withRequestBody(containing("grant_type=authorization_code"))
                .willReturn(okJson(fixture("/zalo/http/oauth-code-token.json"))));
        ZALO.stubFor(post("/oauth").withRequestBody(containing("grant_type=refresh_token"))
                .willReturn(okJson(fixture("/zalo/http/oauth-refreshed-token.json"))));
        ZALO.stubFor(post("/oa-message").willReturn(okJson(fixture("/zalo/http/oa-message-success.json"))));
        ZALO.stubFor(post("/zbs").willReturn(okJson(fixture("/zalo/http/zbs-message-success.json"))));
        ZALO.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/authorize")
                .willReturn(aResponse().withStatus(200)));
    }

    private static String oauthCodeVerifier() {
        return ZALO.findAll(postRequestedFor(urlEqualTo("/oauth"))).stream()
                .map(request -> new String(request.getBody(), StandardCharsets.UTF_8))
                .filter(body -> body.contains("grant_type=authorization_code"))
                .map(body -> formParameter(body, "code_verifier"))
                .findFirst().orElseThrow();
    }

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
        postgres.start();
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(fixture(
                    "/io/github/hoangluongtran0309/vietnam/zalo/postgresql/V1__create_zalo_oa_tokens.sql"));
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot apply the Zalo token migration", e);
        }
        return postgres;
    }

    private static WireMockServer startFakeZalo() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    private static String fixture(String path) {
        try (var input = Objects.requireNonNull(ZaloCaptureEndToEndTest.class.getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String queryParameter(URI uri, String name) {
        return formParameter(uri.getRawQuery(), name);
    }

    private static String formParameter(String form, String name) {
        for (String parameter : form.split("&")) {
            String[] pair = parameter.split("=", 2);
            if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing parameter: " + name);
    }

    private static String signature(String rawBody, String secret) {
        try {
            String input = "app" + rawBody + "1000" + secret;
            return "mac=" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeZaloConfiguration {

        @Bean
        MutableClock testClock() {
            return new MutableClock(INITIAL_TIME);
        }

        @Bean
        ZaloOAuthClient fakeZaloOAuthClient(ZaloOaProperties properties, MutableClock clock,
                                             @Qualifier("zaloHttpClient") HttpClient httpClient) {
            return new HttpZaloOAuthClient(properties.getAppId(), properties.getSecretKey(), httpClient,
                    URI.create(ZALO.baseUrl() + "/oauth"), properties.getRequestTimeout(), clock);
        }

        @Bean
        ZaloOaRegistry fakeZaloOaRegistry(ZaloOaProperties properties, ZaloTokenStore tokenStore,
                                          MutableClock clock,
                                          @Qualifier("zaloHttpClient") HttpClient httpClient) {
            List<ZaloAccountConfig> accounts = properties.getAccounts().entrySet().stream()
                    .map(entry -> new ZaloAccountConfig(entry.getKey(), properties.getAppId(),
                            properties.getSecretKey(), entry.getValue().getOaId(),
                            entry.getValue().getOaSecretKey()))
                    .toList();
            return ZaloOaRegistry.builder(accounts, tokenStore)
                    .httpClient(httpClient)
                    .oauthEndpoint(URI.create(ZALO.baseUrl() + "/oauth"))
                    .messagingEndpoint(URI.create(ZALO.baseUrl() + "/oa-message"))
                    .zbsEndpoint(URI.create(ZALO.baseUrl() + "/zbs"))
                    .requestTimeout(properties.getRequestTimeout())
                    .lockTimeout(properties.getJdbc().getLockTimeout())
                    .clock(clock)
                    .defaultAccount(properties.getDefaultAccount())
                    .build();
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        MutableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }

        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant.get(); }
    }
}
