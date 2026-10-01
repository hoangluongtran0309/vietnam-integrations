package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloToken;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenManager;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app")
                .run(ctx -> {
                    assertThat(ctx).getBean(ZaloTokenStore.class).isInstanceOf(InMemoryZaloTokenStore.class);
                    assertThat(ctx).doesNotHaveBean(ZaloWebhookVerifier.class);
                    assertThat(ctx).doesNotHaveBean(ZaloTokenManager.class);
                });
    }

    @Test
    void registersWebhookVerifierWhenOaSecretKeySet() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.oa-secret-key=secret")
                .run(ctx -> assertThat(ctx).hasSingleBean(ZaloWebhookVerifier.class));
    }

    @Test
    void registersTokenManagerWhenOAuthClientPresent() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.oa-id=oa")
                .withBean(ZaloOAuthClient.class, StubOAuthClient::new)
                .run(ctx -> assertThat(ctx).hasSingleBean(ZaloTokenManager.class));
    }

    @Test
    void failsFastWhenJdbcStoreRequested() {
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.token-store=jdbc")
                .run(ctx -> assertThat(ctx).hasFailed().getFailure()
                        .rootCause().isInstanceOf(ZaloException.class).hasMessageContaining("token-store=jdbc"));
    }

    @Test
    void userTokenStoreWinsEvenWithJdbcSetting() {
        ZaloTokenStore custom = new InMemoryZaloTokenStore();
        runner.withPropertyValues("vietnam.zalo-oa.app-id=app", "vietnam.zalo-oa.token-store=jdbc")
                .withBean(ZaloTokenStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ZaloTokenStore.class)).isSameAs(custom));
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
