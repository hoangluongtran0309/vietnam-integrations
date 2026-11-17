package io.github.hoangluongtran0309.vietnam.demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.autoconfigure.ZaloOaProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
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
@Import(ZaloContractEndToEndTest.FakeZaloConfiguration.class)
class ZaloContractEndToEndTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-11-17T00:00:00Z");
    private static final PostgreSQLContainer POSTGRES = startPostgres();
    private static final WireMockServer ZALO = startFakeZalo();

    @DynamicPropertySource
    static void fakeServices(DynamicPropertyRegistry properties) {
        properties.add("demo.zalo.jdbc-url", POSTGRES::getJdbcUrl);
        properties.add("demo.zalo.jdbc-user", POSTGRES::getUsername);
        properties.add("demo.zalo.jdbc-password", POSTGRES::getPassword);
        properties.add("demo.zalo.authorization-uri", () -> ZALO.baseUrl() + "/authorize");
        properties.add("demo.zalo.redirect-uri", () -> "http://demo.invalid/zalo/oauth/callback");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MutableClock clock;

    @Test
    void exercisesTheDemoAgainstFakeZaloAndPostgresql() throws Exception {
        stubFakeZalo();
        HttpClient browser = browser();

        HttpResponse<String> start = browser.send(get("/zalo/oauth/start?account=cskh"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(start.statusCode()).isEqualTo(302);
        URI authorization = URI.create(start.headers().firstValue("Location").orElseThrow());
        assertThat(authorization.toString()).startsWith(ZALO.baseUrl() + "/authorize?");
        assertThat(queryParameter(authorization, "app_id")).isEqualTo("app");
        assertThat(queryParameter(authorization, "code_challenge")).isNotBlank();
        String state = queryParameter(authorization, "state");
        assertThat(state).isNotBlank();
        assertThat(tokenRows()).isZero();

        HttpResponse<String> callback = browser.send(get("/zalo/oauth/callback?code=" + encode("code+value")
                        + "&oa_id=oa-1&state=" + encode(state)), HttpResponse.BodyHandlers.ofString());
        assertThat(callback.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(callback.body(), "$.oaId")).isEqualTo("oa-1");
        assertThat(token("oa-1")).isEqualTo(new TokenRow(
                "oa-1", "access", "refresh", INITIAL_TIME.plusSeconds(90_000)));
        ZALO.verify(1, postRequestedFor(urlEqualTo("/oauth"))
                .withHeader("secret_key", equalTo("test-app-secret"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("app_id=app"))
                .withRequestBody(containing("code=code%2Bvalue"))
                .withRequestBody(containing("code_verifier=")));

        clock.advance(Duration.ofHours(26));
        HttpResponse<String> consultation = browser.send(appPost(
                        "/zalo/consultation?account=cskh&userId=user-1&text=" + encode("Xin chào"), ""),
                HttpResponse.BodyHandlers.ofString());
        assertThat(consultation.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(consultation.body(), "$.messageId")).isEqualTo("msg-1");
        assertThat(token("oa-1")).isEqualTo(new TokenRow(
                "oa-1", "new-access-1", "new-refresh-1", clock.instant().plusSeconds(90_000)));
        ZALO.verify(1, postRequestedFor(urlEqualTo("/oauth"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(containing("refresh_token=refresh")));
        ZALO.verify(1, postRequestedFor(urlEqualTo("/oa-message"))
                .withHeader("access_token", equalTo("new-access-1"))
                .withRequestBody(matchingJsonPath("$.recipient.user_id", equalTo("user-1")))
                .withRequestBody(matchingJsonPath("$.message.text", equalTo("Xin chào"))));

        String zbsRequest = new Gson().toJson(Map.of(
                "phone", "(+84) 912.345-678",
                "templateId", "template-1",
                "templateData", Map.of("amount", 100, "name", "Nguyễn Văn A"),
                "trackingId", "order-123"));
        HttpResponse<String> zbs = browser.send(appPost("/zalo/zbs?account=cskh", zbsRequest),
                HttpResponse.BodyHandlers.ofString());
        assertThat(zbs.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(zbs.body(), "$.messageId")).isEqualTo("zbs-1");
        ZALO.verify(1, postRequestedFor(urlEqualTo("/zbs"))
                .withHeader("access_token", equalTo("new-access-1"))
                .withRequestBody(matchingJsonPath("$.phone", equalTo("84912345678")))
                .withRequestBody(matchingJsonPath("$.template_id", equalTo("template-1"))));
        assertThat(tokenRows()).isOne();
        assertThat(token("oa-1").refreshToken()).isEqualTo("new-refresh-1");

        String webhook = fixture("/zalo/webhook/user-text.json");
        HttpResponse<String> accepted = browser.send(webhook(webhook,
                        signature(webhook, "test-oa-secret-2")), HttpResponse.BodyHandlers.ofString());
        assertThat(accepted.statusCode()).isEqualTo(200);
        HttpResponse<String> monitored = browser.send(get("/zalo/webhook/last"),
                HttpResponse.BodyHandlers.ofString());
        assertThat(monitored.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(monitored.body(), "$.oaId")).isEqualTo("oa-2");
        assertThat(tokenRows()).isOne();
        assertThat(token("oa-1").refreshToken()).isEqualTo("new-refresh-1");

        HttpResponse<String> rejected = browser.send(webhook(webhook,
                        signature(webhook, "test-oa-secret-1")), HttpResponse.BodyHandlers.ofString());
        assertThat(rejected.statusCode()).isEqualTo(401);
        assertThat(tokenRows()).isOne();
        assertThat(token("oa-1").refreshToken()).isEqualTo("new-refresh-1");
    }

    @AfterAll
    static void stopFakeServices() {
        ZALO.stop();
        POSTGRES.stop();
    }

    private HttpClient browser() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(appUri(path)).GET().build();
    }

    private HttpRequest appPost(String path, String body) {
        return HttpRequest.newBuilder(appUri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest webhook(String body, String signature) {
        return HttpRequest.newBuilder(appUri("/webhooks/zalo"))
                .header("Content-Type", "application/json")
                .header("X-ZEvent-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private URI appUri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private int tokenRows() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM zalo_oa_tokens")) {
            result.next();
            return result.getInt(1);
        }
    }

    private TokenRow token(String oaId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT oa_id, access_token, refresh_token, "
                     + "access_token_expires_at FROM zalo_oa_tokens WHERE oa_id = ?")) {
            statement.setString(1, oaId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return new TokenRow(result.getString("oa_id"), result.getString("access_token"),
                        result.getString("refresh_token"), result.getTimestamp("access_token_expires_at").toInstant());
            }
        }
    }

    private static void stubFakeZalo() {
        ZALO.resetAll();
        ZALO.stubFor(post("/oauth")
                .withRequestBody(containing("grant_type=authorization_code"))
                .willReturn(okJson(fixture("/zalo/http/oauth-code-token.json"))));
        ZALO.stubFor(post("/oauth")
                .withRequestBody(containing("grant_type=refresh_token"))
                .willReturn(okJson(fixture("/zalo/http/oauth-refreshed-token.json"))));
        ZALO.stubFor(post("/oa-message")
                .willReturn(okJson(fixture("/zalo/http/oa-message-success.json"))));
        ZALO.stubFor(post("/zbs")
                .willReturn(okJson(fixture("/zalo/http/zbs-message-success.json"))));
        ZALO.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/authorize")
                .willReturn(aResponse().withStatus(200)));
    }

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
        postgres.start();
        migrate(postgres);
        return postgres;
    }

    private static void migrate(PostgreSQLContainer postgres) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        String migration = fixture(
                "/io/github/hoangluongtran0309/vietnam/zalo/postgresql/V1__create_zalo_oa_tokens.sql");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(migration);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot apply the Zalo token migration", e);
        }
    }

    private static WireMockServer startFakeZalo() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    private static String fixture(String path) {
        try (var input = Objects.requireNonNull(ZaloContractEndToEndTest.class.getResourceAsStream(path), path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String queryParameter(URI uri, String name) {
        for (String parameter : uri.getRawQuery().split("&")) {
            String[] pair = parameter.split("=", 2);
            if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing query parameter: " + name);
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

    private record TokenRow(String oaId, String accessToken, String refreshToken, Instant expiresAt) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeZaloConfiguration {

        @Bean
        MutableClock testClock() {
            return new MutableClock(INITIAL_TIME);
        }

        @Bean
        ZaloOAuthClient fakeZaloOAuthClient(ZaloOaProperties properties, MutableClock clock) {
            return new HttpZaloOAuthClient(properties.getAppId(), properties.getSecretKey(),
                    HttpClient.newHttpClient(), URI.create(ZALO.baseUrl() + "/oauth"),
                    properties.getRequestTimeout(), clock);
        }

        @Bean
        ZaloOaRegistry fakeZaloOaRegistry(ZaloOaProperties properties, ZaloTokenStore tokenStore,
                                          MutableClock clock) {
            List<ZaloAccountConfig> accounts = properties.getAccounts().entrySet().stream()
                    .map(entry -> new ZaloAccountConfig(entry.getKey(), properties.getAppId(),
                            properties.getSecretKey(), entry.getValue().getOaId(),
                            entry.getValue().getOaSecretKey()))
                    .toList();
            return ZaloOaRegistry.builder(accounts, tokenStore)
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

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("Only UTC is supported by this test clock");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
