package io.github.hoangluongtran0309.vietnam.zalo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloOaRegistryTest {

    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

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
    void accountsWithTheSameAppShareOneOAuthClient() {
        ZaloOaRegistry registry = registry(List.of(
                account("first", "app", "secret", "oa-1", "shared-oa-secret"),
                account("second", "app", "secret", "oa-2", "shared-oa-secret")));

        assertThat(registry.oauthClient("first")).isSameAs(registry.oauthClient("second"));
        assertThat(registry.oa("first").tokenManager()).isNotSameAs(registry.oa("second").tokenManager());
        assertThat(registry.oa("first").messaging()).isNotSameAs(registry.oa("second").messaging());
        assertThat(registry.oa("first").zbs()).isNotSameAs(registry.oa("second").zbs());
    }

    @Test
    void accountsWithDifferentAppsUseDifferentOAuthClients() {
        ZaloOaRegistry registry = registry(List.of(
                account("first", "app-1", "secret-1", "oa-1", null),
                account("second", "app-2", "secret-2", "oa-2", null)));

        assertThat(registry.oauthClient("first")).isNotSameAs(registry.oauthClient("second"));
    }

    @Test
    void refreshingOneAccountDoesNotChangeAnotherAccountsToken() {
        server.stubFor(post("/oauth")
                .withRequestBody(containing("refresh_token=refresh-1"))
                .willReturn(okJson(fixture("oauth-refreshed-token.json"))));
        InMemoryZaloTokenStore store = new InMemoryZaloTokenStore();
        ZaloToken firstBefore = new ZaloToken("old-access-1", "refresh-1", NOW.plusSeconds(60));
        ZaloToken secondBefore = new ZaloToken("old-access-2", "refresh-2", NOW.plusSeconds(60));
        store.save("oa-1", firstBefore);
        store.save("oa-2", secondBefore);
        ZaloOaRegistry registry = registry(List.of(
                account("first", "app", "secret", "oa-1", null),
                account("second", "app", "secret", "oa-2", null)), store);

        assertThat(registry.oa("first").tokenManager().accessToken()).isEqualTo("new-access-1");

        assertThat(store.load("oa-1")).contains(new ZaloToken(
                "new-access-1", "new-refresh-1", NOW.plusSeconds(90_000)));
        assertThat(store.load("oa-2")).contains(secondBefore);
        server.verify(1, postRequestedFor(urlEqualTo("/oauth"))
                .withRequestBody(containing("refresh_token=refresh-1")));
        server.verify(0, postRequestedFor(urlEqualTo("/oauth"))
                .withRequestBody(containing("refresh_token=refresh-2")));
    }

    @Test
    void looksUpTheSameAccountAndPreservesConfigurationOrder() {
        ZaloOaRegistry registry = registryBuilder(List.of(
                account("first", "app", "secret", "oa-1", null),
                account("second", "app", "secret", "oa-2", null)), new InMemoryZaloTokenStore())
                .defaultAccount("second")
                .build();

        assertThat(registry.oa("second")).isSameAs(registry.byOaId("oa-2"));
        assertThat(registry.findByOaId("oa-2")).containsSame(registry.oa("second"));
        assertThat(registry.findByOaId("missing")).isEmpty();
        assertThat(registry.accounts()).extracting(ZaloOa::alias).containsExactly("first", "second");
        assertThat(registry.defaultOa()).containsSame(registry.oa("second"));
        assertThatThrownBy(() -> registry.accounts().remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void soleAccountIsTheDefaultAndMultipleAccountsNeedAnExplicitDefault() {
        ZaloOaRegistry sole = registry(List.of(account("only", "app", "secret", "oa", null)));
        ZaloOaRegistry multiple = registry(List.of(
                account("first", "app", "secret", "oa-1", null),
                account("second", "app", "secret", "oa-2", null)));

        assertThat(sole.defaultOa()).containsSame(sole.oa("only"));
        assertThat(multiple.defaultOa()).isEmpty();
    }

    @Test
    void missingLookupsNameAvailableAliases() {
        ZaloOaRegistry registry = registry(List.of(
                account("first", "app", "secret", "oa-1", null),
                account("second", "app", "secret", "oa-2", null)));

        assertThatThrownBy(() -> registry.oa("missing"))
                .isInstanceOf(ZaloException.class)
                .hasMessageContaining("alias 'missing'")
                .hasMessageContaining("[first, second]");
        assertThatThrownBy(() -> registry.byOaId("oa-missing"))
                .isInstanceOf(ZaloException.class)
                .hasMessageContaining("OA id 'oa-missing'")
                .hasMessageContaining("[first, second]");
    }

    @Test
    void rejectsDuplicateAliasesAndOaIds() {
        assertThatThrownBy(() -> registry(List.of(
                account("duplicate", "app", "secret", "oa-1", null),
                account("duplicate", "app", "secret", "oa-2", null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("alias");
        assertThatThrownBy(() -> registry(List.of(
                account("first", "app", "secret", "same-oa", null),
                account("second", "app", "secret", "same-oa", null))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("OA id");
    }

    @Test
    void rejectsDifferentSecretsForTheSameApp() {
        assertThatThrownBy(() -> registry(List.of(
                account("first", "app", "secret-1", "oa-1", null),
                account("second", "app", "secret-2", "oa-2", null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appId 'app'")
                .hasMessageContaining("same secretKey");
    }

    @Test
    void rejectsInvalidDefaultAndTimeoutsAtBuildTime() {
        List<ZaloAccountConfig> accounts = List.of(account("first", "app", "secret", "oa", null));

        assertThatThrownBy(() -> registryBuilder(accounts, new InMemoryZaloTokenStore())
                .defaultAccount("missing").build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("defaultAccount");
        assertThatThrownBy(() -> registryBuilder(accounts, new InMemoryZaloTokenStore())
                .requestTimeout(Duration.ofSeconds(10)).lockTimeout(Duration.ofSeconds(10)).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lockTimeout").hasMessageContaining("requestTimeout");
        assertThatThrownBy(() -> registryBuilder(accounts, new InMemoryZaloTokenStore())
                .requestTimeout(Duration.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requestTimeout");
    }

    private ZaloOaRegistry registry(List<ZaloAccountConfig> accounts) {
        return registry(accounts, new InMemoryZaloTokenStore());
    }

    private ZaloOaRegistry registry(List<ZaloAccountConfig> accounts, ZaloTokenStore store) {
        return registryBuilder(accounts, store).build();
    }

    private ZaloOaRegistry.Builder registryBuilder(List<ZaloAccountConfig> accounts, ZaloTokenStore store) {
        return ZaloOaRegistry.builder(accounts, store)
                .httpClient(HttpClient.newHttpClient())
                .oauthEndpoint(uri("/oauth"))
                .messagingEndpoint(uri("/oa-message"))
                .zbsEndpoint(uri("/zbs"))
                .requestTimeout(Duration.ofSeconds(2))
                .lockTimeout(Duration.ofSeconds(3))
                .clock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private URI uri(String path) {
        return URI.create(server.baseUrl() + path);
    }

    private static String fixture(String name) {
        try (var input = Objects.requireNonNull(
                ZaloOaRegistryTest.class.getResourceAsStream("/zalo/http/" + name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ZaloAccountConfig account(String alias, String appId, String secretKey, String oaId,
                                             String oaSecretKey) {
        return new ZaloAccountConfig(alias, appId, secretKey, oaId, oaSecretKey);
    }
}
