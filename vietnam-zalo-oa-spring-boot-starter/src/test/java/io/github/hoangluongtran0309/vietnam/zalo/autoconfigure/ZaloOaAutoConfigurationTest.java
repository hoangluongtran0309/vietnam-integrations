package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloToken;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenManager;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.JdbcZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloZbsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;

class ZaloOaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZaloOaAutoConfiguration.class));

    @Test
    void backsOffWithoutAppId() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ZaloTokenStore.class));
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ZaloTokenStore.class));
    }

    @Test
    void registersInMemoryStoreByDefault() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa")
                .run(ctx -> {
                    assertThat(ctx).getBean(ZaloTokenStore.class).isInstanceOf(InMemoryZaloTokenStore.class);
                    assertThat(ctx).doesNotHaveBean(ZaloWebhookVerifier.class);
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                });
    }

    @Test
    void registersWebhookVerifierWhenOaSecretKeySet() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=app-secret",
                        "vietnam.zalo-oa.oa-id=oa", "vietnam.zalo-oa.oa-secret-key=secret")
                .run(ctx -> assertThat(ctx).hasSingleBean(ZaloWebhookVerifier.class));
    }

    @Test
    void registersTokenManagerWhenOAuthClientPresent() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa")
                .withBean(ZaloOAuthClient.class, StubOAuthClient::new)
                .run(ctx -> assertThat(ctx).hasSingleBean(ZaloTokenManager.class));
    }

    @Test
    void registersHttpClientsWhenCredentialsAreComplete() {
        runner.withPropertyValues(
                        "vietnam.zalo-oa.app-id=app",
                        "vietnam.zalo-oa.secret-key=app-secret",
                        "vietnam.zalo-oa.oa-id=oa")
                .run(ctx -> {
                    assertThat(ctx).getBean(ZaloOAuthClient.class).isInstanceOf(HttpZaloOAuthClient.class);
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);
                });
    }

    @Test
    void failsFastWhenJdbcStoreRequested() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa",
                        "vietnam.zalo-oa.token-store=jdbc")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure()
                        .rootCause().isInstanceOf(ZaloException.class).hasMessageContaining("token-store=jdbc"));
    }

    @Test
    void userTokenStoreWinsEvenWithJdbcSetting() {
        ZaloTokenStore custom = new InMemoryZaloTokenStore();
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa",
                        "vietnam.zalo-oa.token-store=jdbc")
                .withBean(ZaloTokenStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ZaloTokenStore.class)).isSameAs(custom));
    }

    @Test
    void registersJdbcStoreWhenDataSourceExists() {
        DataSource unused = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    throw new UnsupportedOperationException("not used during auto-configuration");
                });
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa",
                        "vietnam.zalo-oa.token-store=jdbc")
                .withBean(DataSource.class, () -> unused)
                .run(ctx -> assertThat(ctx).getBean(ZaloTokenStore.class).isInstanceOf(JdbcZaloTokenStore.class));
    }

    @Test
    void bindsJdbcLockTimeout() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa", "vietnam.zalo-oa.jdbc.lock-timeout=20s")
                .run(ctx -> assertThat(ctx.getBean(ZaloOaProperties.class).getJdbc().getLockTimeout())
                        .isEqualTo(Duration.ofSeconds(20)));
    }

    @Test
    void failsFastWhenJdbcLockTimeoutIsNotGreaterThanRequestTimeout() {
        assertInvalidLockTimeout("10s");
        assertInvalidLockTimeout("9s");
    }

    private void assertInvalidLockTimeout(String lockTimeout) {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa", "vietnam.zalo-oa.request-timeout=10s",
                        "vietnam.zalo-oa.jdbc.lock-timeout=" + lockTimeout)
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.jdbc.lock-timeout")
                        .hasMessageContaining("vietnam.zalo-oa.request-timeout"));
    }

    @Test
    void failsFastWhenSecretKeyMissing() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.oa-id=oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class).hasMessageContaining("secret-key"));
    }

    @Test
    void failsFastWhenAppIdIsBlank() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=", "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class).hasMessageContaining("vietnam.zalo-oa.app-id"));
    }

    @Test
    void failsFastWhenOaIdMissing() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=private-app-secret")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                        .isInstanceOf(ZaloException.class)
                        .hasMessageContaining("vietnam.zalo-oa.oa-id")
                        .hasMessageNotContaining("private-app-secret"));
    }

    @Test
    void userTokenManagerDoesNotRequireOaId() {
        ZaloTokenManager custom = new ZaloTokenManager("custom-oa", new InMemoryZaloTokenStore(),
                new StubOAuthClient(), Clock.systemUTC());

        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.secret-key=secret")
                .withBean(ZaloTokenManager.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx.getBean(ZaloTokenManager.class)).isSameAs(custom);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);
                });
    }

    @Test
    void nonWebApplicationSkipsWebhookControllerAndRegistersOtherBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ZaloOaAutoConfiguration.class, ZaloWebhookAutoConfiguration.class))
                .withPropertyValues(
                        "vietnam.zalo-oa.app-id=app",
                        "vietnam.zalo-oa.secret-key=secret",
                        "vietnam.zalo-oa.oa-id=oa",
                        "vietnam.zalo-oa.oa-secret-key=oa-secret",
                        "vietnam.zalo-oa.webhook.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(ZaloWebhookController.class);
                    assertThat(ctx).hasSingleBean(ZaloTokenManager.class);
                    assertThat(ctx).hasSingleBean(ZaloOaClient.class);
                    assertThat(ctx).hasSingleBean(ZaloZbsClient.class);
                });
    }

    private static final class StubOAuthClient implements ZaloOAuthClient {

        @Override
        public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ZaloToken refresh(String refreshToken) {
            throw new UnsupportedOperationException();
        }
    }
}
