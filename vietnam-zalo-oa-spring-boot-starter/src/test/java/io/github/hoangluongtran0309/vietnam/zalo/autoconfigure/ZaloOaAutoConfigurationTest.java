package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.JdbcZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloOaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZaloOaAutoConfiguration.class));

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
}
