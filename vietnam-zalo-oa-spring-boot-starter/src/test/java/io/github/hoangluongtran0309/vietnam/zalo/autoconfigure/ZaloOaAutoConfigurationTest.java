package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.JdbcZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloConsultationTextRequest;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenManager;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloZbsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloOaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZaloOaAutoConfiguration.class));

    @Test
    void registersNamedHttpClientWithConfiguredConnectTimeout() {
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.connect-timeout=3s")
                .run(ctx -> assertThat(ctx.getBean("zaloHttpClient", HttpClient.class).connectTimeout())
                        .contains(Duration.ofSeconds(3)));
    }

    @Test
    void namedHttpClientIsNotADefaultCandidateForTypeInjection() {
        HttpClient applicationClient = HttpClient.newHttpClient();
        runner.withPropertyValues(legacyAccount())
                .withBean("applicationHttpClient", HttpClient.class, () -> applicationClient)
                .run(ctx -> {
                    assertThat(ctx.getBean(HttpClient.class)).isSameAs(applicationClient);
                    assertThat(ctx.getBean("zaloHttpClient", HttpClient.class)).isNotSameAs(applicationClient);
                });
    }

    @Test
    void applicationProvidedNamedHttpClientIsUsedByRegistryAndOAuthClient() {
        RecordingHttpClient custom = new RecordingHttpClient();
        runner.withPropertyValues(legacyAccount())
                .withBean("zaloHttpClient", HttpClient.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx.getBean("zaloHttpClient", HttpClient.class)).isSameAs(custom);
                    ctx.getBean(ZaloOAuthClient.class).exchangeAuthorizationCode("code", "verifier");
                    ctx.getBean(ZaloTokenStore.class).save("oa", new io.github.hoangluongtran0309.vietnam.zalo.ZaloToken(
                            "access", "refresh", Instant.parse("2099-01-01T00:00:00Z")));
                    ctx.getBean(ZaloOaRegistry.class).oa("default").messaging()
                            .sendConsultationText(new ZaloConsultationTextRequest("user", "Hello"));
                    assertThat(custom.requests).extracting(request -> request.uri().getHost())
                            .containsExactly("oauth.zaloapp.com", "openapi.zalo.me");
                });
    }

    @Test
    void failsFastWithoutLegacyOrMapAccountConfiguration() {
        runner.run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                .isInstanceOf(ZaloException.class)
                .hasMessageContaining("vietnam.zalo-oa.accounts")
                .hasMessageContaining("vietnam.zalo-oa.oa-id"));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("vietnam.zalo-oa.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(ZaloTokenStore.class);
                    assertThat(ctx).doesNotHaveBean(ZaloOaRegistry.class);
                });
    }

    @Test
    void legacySingleAccountConfigurationRegistersRegistryAndConvenienceBeans() {
        runner.withPropertyValues(legacyAccount())
                .run(ctx -> {
                    assertThat(ctx).getBean(ZaloTokenStore.class).isInstanceOf(InMemoryZaloTokenStore.class);
                    assertThat(ctx).hasSingleBean(ZaloOaRegistry.class);
                    assertThat(ctx).getBean(ZaloOAuthClient.class).isInstanceOf(HttpZaloOAuthClient.class);
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);

                    ZaloOaRegistry registry = ctx.getBean(ZaloOaRegistry.class);
                    assertThat(registry.accounts()).extracting(account -> account.alias())
                            .containsExactly("default");
                    assertThat(ctx.getBean(ZaloTokenManager.class))
                            .isSameAs(registry.oa("default").tokenManager());
                    assertThat(ctx.getBean(ZaloOaClient.class))
                            .isSameAs(registry.oa("default").messaging());
                    assertThat(ctx.getBean(ZaloZbsClient.class))
                            .isSameAs(registry.oa("default").zbs());
                });
    }

    @Test
    void usesAnApplicationProvidedClockAcrossTokenComponents() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-17T00:00:00Z"), ZoneOffset.UTC);

        runner.withPropertyValues(legacyAccount())
                .withBean(Clock.class, () -> clock)
                .run(ctx -> {
                    assertThat(ctx.getBean(Clock.class)).isSameAs(clock);
                    assertThat(ctx.getBean(ZaloOAuthClient.class))
                            .extracting("clock")
                            .isSameAs(clock);
                    assertThat(ctx.getBean(ZaloOaRegistry.class).oa("default").tokenManager())
                            .extracting("clock")
                            .isSameAs(clock);
                });
    }

    @Test
    void twoAccountsWithoutDefaultOnlyRegisterTheRegistry() {
        runner.withPropertyValues(twoAccounts())
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ZaloOaRegistry.class);
                    assertThat(ctx).doesNotHaveBean(ZaloTokenManager.class);
                    assertThat(ctx).doesNotHaveBean(ZaloOaClient.class);
                    assertThat(ctx).doesNotHaveBean(ZaloZbsClient.class);
                    assertThat(ctx.getBean(ZaloOaRegistry.class).accounts())
                            .extracting(account -> account.alias())
                            .containsExactly("cskh", "marketing");
                });
    }

    @Test
    void soleMapAccountCanProvideItsOwnApplicationCredentialsAndBecomesDefault() {
        runner.withPropertyValues(
                        "vietnam.zalo-oa.accounts.sole.oa-id=oa-sole",
                        "vietnam.zalo-oa.accounts.sole.app-id=sole-app",
                        "vietnam.zalo-oa.accounts.sole.secret-key=sole-secret")
                .run(ctx -> {
                    ZaloOaRegistry registry = ctx.getBean(ZaloOaRegistry.class);
                    assertThat(registry.defaultOa()).containsSame(registry.oa("sole"));
                    assertThat(registry.oa("sole").appId()).isEqualTo("sole-app");
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);
                });
    }

    @Test
    void explicitDefaultAccountRegistersConvenienceBeansForThatAccount() {
        runner.withPropertyValues(twoAccounts())
                .withPropertyValues("vietnam.zalo-oa.default-account=marketing")
                .run(ctx -> {
                    ZaloOaRegistry registry = ctx.getBean(ZaloOaRegistry.class);
                    assertThat(registry.defaultOa()).containsSame(registry.oa("marketing"));
                    assertThat(ctx.getBean(ZaloTokenManager.class))
                            .isSameAs(registry.oa("marketing").tokenManager());
                    assertThat(ctx.getBean(ZaloOaClient.class))
                            .isSameAs(registry.oa("marketing").messaging());
                    assertThat(ctx.getBean(ZaloZbsClient.class))
                            .isSameAs(registry.oa("marketing").zbs());
                });
    }

    @Test
    void accountCredentialsOverrideRootDefaults() {
        runner.withPropertyValues(twoAccounts())
                .withPropertyValues(
                        "vietnam.zalo-oa.accounts.marketing.app-id=marketing-app",
                        "vietnam.zalo-oa.accounts.marketing.secret-key=marketing-secret")
                .run(ctx -> {
                    ZaloOaRegistry registry = ctx.getBean(ZaloOaRegistry.class);
                    assertThat(registry.oa("cskh").appId()).isEqualTo("shared-app");
                    assertThat(registry.oa("marketing").appId()).isEqualTo("marketing-app");
                });
    }

    @Test
    void customRegistryWinsAndStillSuppliesConvenienceBeans() {
        ZaloOaRegistry custom = ZaloOaRegistry.builder(List.of(
                        new ZaloAccountConfig("custom", "app", "secret", "oa", null)),
                new InMemoryZaloTokenStore()).build();

        runner.withBean(ZaloOaRegistry.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx.getBean(ZaloOaRegistry.class)).isSameAs(custom);
                    assertThat(ctx.getBean(ZaloTokenManager.class))
                            .isSameAs(custom.oa("custom").tokenManager());
                    assertThat(ctx.getBean(ZaloOaClient.class))
                            .isSameAs(custom.oa("custom").messaging());
                    assertThat(ctx.getBean(ZaloZbsClient.class))
                            .isSameAs(custom.oa("custom").zbs());
                });
    }

    @Test
    void userConvenienceBeanWins() {
        ZaloOaClient custom = request -> {
            throw new UnsupportedOperationException();
        };

        runner.withPropertyValues(legacyAccount())
                .withBean(ZaloOaClient.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ZaloOaClient.class)).isSameAs(custom));
    }

    @Test
    void userOAuthClientWinsForLegacyConfiguration() {
        ZaloOAuthClient custom = new ZaloOAuthClient() {
            @Override
            public io.github.hoangluongtran0309.vietnam.zalo.ZaloToken exchangeAuthorizationCode(
                    String code, String codeVerifier) {
                throw new UnsupportedOperationException();
            }

            @Override
            public io.github.hoangluongtran0309.vietnam.zalo.ZaloToken refresh(String refreshToken) {
                throw new UnsupportedOperationException();
            }
        };

        runner.withPropertyValues(legacyAccount())
                .withBean(ZaloOAuthClient.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ZaloOAuthClient.class)).isSameAs(custom));
    }

    @Test
    void failsFastWhenBothConfigurationStylesAreMixed() {
        runner.withPropertyValues(twoAccounts())
                .withPropertyValues("vietnam.zalo-oa.oa-id=legacy-oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.accounts")
                        .hasMessageContaining("vietnam.zalo-oa.oa-id")
                        .hasMessageContaining("not both"));
    }

    @Test
    void failsFastWhenDefaultAccountIsUnknown() {
        runner.withPropertyValues(twoAccounts())
                .withPropertyValues("vietnam.zalo-oa.default-account=unknown")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.default-account")
                        .hasMessageContaining("unknown")
                        .hasMessageContaining("cskh", "marketing"));
    }

    @Test
    void failsFastWithExactMapPropertyNames() {
        runner.withPropertyValues(
                        "vietnam.zalo-oa.app-id=app",
                        "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.accounts.cskh.oa-id=")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.accounts.cskh.oa-id"));

        runner.withPropertyValues("vietnam.zalo-oa.accounts.cskh.oa-id=oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.accounts.cskh.app-id")
                        .hasMessageContaining("vietnam.zalo-oa.app-id"));

        runner.withPropertyValues(
                        "vietnam.zalo-oa.app-id=app",
                        "vietnam.zalo-oa.accounts.cskh.oa-id=oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.accounts.cskh.secret-key")
                        .hasMessageContaining("vietnam.zalo-oa.secret-key"));
    }

    @Test
    void failsFastWhenLegacySecretKeyIsMissing() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.oa-id=oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.secret-key"));
    }

    @Test
    void failsFastWhenJdbcStoreRequestedWithoutDataSource() {
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.token-store=jdbc")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class).hasMessageContaining("token-store=jdbc"));
    }

    @Test
    void customTokenStoreWinsEvenWithJdbcSetting() {
        ZaloTokenStore custom = new InMemoryZaloTokenStore();
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.token-store=jdbc")
                .withBean(ZaloTokenStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ZaloTokenStore.class)).isSameAs(custom));
    }

    @Test
    void jdbcStoreIsSharedByEveryAccount() {
        DataSource unused = unusedDataSource();
        runner.withPropertyValues(twoAccounts())
                .withPropertyValues("vietnam.zalo-oa.token-store=jdbc")
                .withBean(DataSource.class, () -> unused)
                .run(ctx -> {
                    ZaloTokenStore store = ctx.getBean(ZaloTokenStore.class);
                    ZaloOaRegistry registry = ctx.getBean(ZaloOaRegistry.class);
                    assertThat(store).isInstanceOf(JdbcZaloTokenStore.class);
                    assertThat(registry.accounts())
                            .extracting(account -> account.tokenManager())
                            .extracting("store")
                            .containsOnly(store);
                });
    }

    @Test
    void bindsJdbcLockTimeout() {
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.jdbc.lock-timeout=20s")
                .run(ctx -> assertThat(ctx.getBean(ZaloOaProperties.class).getJdbc().getLockTimeout())
                        .isEqualTo(Duration.ofSeconds(20)));
    }

    @Test
    void failsFastWhenJdbcLockTimeoutIsNotGreaterThanRequestTimeout() {
        assertInvalidLockTimeout("10s");
        assertInvalidLockTimeout("9s");
    }

    private void assertInvalidLockTimeout(String lockTimeout) {
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.request-timeout=10s",
                        "vietnam.zalo-oa.jdbc.lock-timeout=" + lockTimeout)
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.jdbc.lock-timeout")
                        .hasMessageContaining("vietnam.zalo-oa.request-timeout"));
    }

    @Test
    void nonWebApplicationStillRegistersAccountBeans() {
        runner.withPropertyValues(legacyAccount())
                .withPropertyValues("vietnam.zalo-oa.webhook.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ZaloOaRegistry.class);
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);
                });
    }

    private static String[] legacyAccount() {
        return new String[] {
                "vietnam.zalo-oa.app-id=app",
                "vietnam.zalo-oa.secret-key=secret",
                "vietnam.zalo-oa.oa-id=oa"
        };
    }

    private static String[] twoAccounts() {
        return new String[] {
                "vietnam.zalo-oa.app-id=shared-app",
                "vietnam.zalo-oa.secret-key=shared-secret",
                "vietnam.zalo-oa.accounts.cskh.oa-id=oa-cskh",
                "vietnam.zalo-oa.accounts.marketing.oa-id=oa-marketing"
        };
    }

    private DataSource unusedDataSource() {
        return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {DataSource.class}, (proxy, method, args) -> {
                    throw new UnsupportedOperationException("not used during auto-configuration");
                });
    }

    private static final class RecordingHttpClient extends HttpClient {
        private final List<HttpRequest> requests = new ArrayList<>();

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            requests.add(request);
            String json = request.uri().getHost().startsWith("oauth")
                    ? "{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":\"3600\"}"
                    : "{\"error\":0,\"data\":{\"message_id\":\"m\",\"sent_time\":\"1000\"}}";
            return response(request, handler, json);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                 HttpResponse.BodyHandler<T> handler) {
            return CompletableFuture.completedFuture(send(request, handler));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                 HttpResponse.BodyHandler<T> handler,
                                                                 HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, handler);
        }

        private static <T> HttpResponse<T> response(HttpRequest request, HttpResponse.BodyHandler<T> handler,
                                                     String body) {
            HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("application/json")),
                    (name, value) -> true);
            HttpResponse.ResponseInfo info = new HttpResponse.ResponseInfo() {
                public int statusCode() { return 200; }
                public HttpHeaders headers() { return headers; }
                public Version version() { return Version.HTTP_1_1; }
            };
            HttpResponse.BodySubscriber<T> subscriber = handler.apply(info);
            subscriber.onSubscribe(new Flow.Subscription() {
                public void request(long n) { }
                public void cancel() { }
            });
            subscriber.onNext(List.of(ByteBuffer.wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            subscriber.onComplete();
            T converted = subscriber.getBody().toCompletableFuture().join();
            return new HttpResponse<>() {
                public int statusCode() { return 200; }
                public HttpRequest request() { return request; }
                public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
                public HttpHeaders headers() { return headers; }
                public T body() { return converted; }
                public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
                public URI uri() { return request.uri(); }
                public Version version() { return Version.HTTP_1_1; }
            };
        }

        public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        public Optional<Duration> connectTimeout() { return Optional.empty(); }
        public Redirect followRedirects() { return Redirect.NEVER; }
        public Optional<ProxySelector> proxy() { return Optional.empty(); }
        public SSLContext sslContext() {
            try { return SSLContext.getDefault(); } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
        }
        public SSLParameters sslParameters() { return new SSLParameters(); }
        public Optional<Authenticator> authenticator() { return Optional.empty(); }
        public Version version() { return Version.HTTP_1_1; }
        public Optional<Executor> executor() { return Optional.empty(); }
        public WebSocket.Builder newWebSocketBuilder() { throw new UnsupportedOperationException(); }
    }
}
