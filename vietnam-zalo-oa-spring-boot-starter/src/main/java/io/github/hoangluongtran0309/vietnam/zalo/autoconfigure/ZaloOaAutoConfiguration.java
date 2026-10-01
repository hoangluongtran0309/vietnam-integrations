package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenManager;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
@EnableConfigurationProperties(ZaloOaProperties.class)
public class ZaloOaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZaloTokenStore zaloTokenStore(ZaloOaProperties properties) {
        if (properties.getTokenStore() == ZaloOaProperties.TokenStoreType.JDBC) {
            // TODO(v0.2): JdbcZaloTokenStore (see docs/adr/0004-zalo-token-store.md)
            throw new ZaloException("vietnam.zalo-oa.token-store=jdbc is not implemented yet. "
                    + "Use token-store=memory (single instance only) or define your own ZaloTokenStore bean.");
        }
        return new InMemoryZaloTokenStore();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "oa-secret-key")
    ZaloWebhookVerifier zaloWebhookVerifier(ZaloOaProperties properties) {
        return new ZaloWebhookVerifier(properties.getAppId(), properties.getOaSecretKey());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZaloOAuthClient.class)
    ZaloTokenManager zaloTokenManager(ZaloOaProperties properties, ZaloTokenStore store, ZaloOAuthClient oauth) {
        return new ZaloTokenManager(properties.getOaId(), store, oauth, Clock.systemUTC());
    }
}
