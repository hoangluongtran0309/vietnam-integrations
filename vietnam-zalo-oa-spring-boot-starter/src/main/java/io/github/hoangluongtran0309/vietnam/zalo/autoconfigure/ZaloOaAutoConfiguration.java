package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.Clock;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ZaloOaProperties.class)
public class ZaloOaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZaloConfigurationValidator zaloConfigurationValidator(ZaloOaProperties properties,
                                                           ObjectProvider<ZaloTokenManager> tokenManagers) {
        return new ZaloConfigurationValidator(properties, tokenManagers);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    ZaloTokenStore zaloTokenStore(ZaloConfigurationValidator validator, ZaloOaProperties properties,
                                  ObjectProvider<DataSource> dataSources) {
        if (properties.getTokenStore() == ZaloOaProperties.TokenStoreType.JDBC) {
            DataSource dataSource = dataSources.getIfAvailable();
            if (dataSource == null) {
                throw new ZaloException("vietnam.zalo-oa.token-store=jdbc requires a DataSource bean and the "
                        + "versioned PostgreSQL schema. Define a DataSource or provide your own ZaloTokenStore bean.");
            }
            return new JdbcZaloTokenStore(dataSource);
        }
        return new InMemoryZaloTokenStore();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "secret-key")
    ZaloOAuthClient zaloOAuthClient(ZaloConfigurationValidator validator, ZaloOaProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloOAuthClient(properties.getAppId(), properties.getSecretKey(), httpClient,
                HttpZaloOAuthClient.DEFAULT_ENDPOINT, properties.getRequestTimeout(), Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "oa-secret-key")
    ZaloWebhookVerifier zaloWebhookVerifier(ZaloConfigurationValidator validator, ZaloOaProperties properties) {
        return new ZaloWebhookVerifier(properties.getAppId(), properties.getOaSecretKey());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "oa-id")
    ZaloTokenManager zaloTokenManager(ZaloConfigurationValidator validator, ZaloOaProperties properties,
                                      ZaloTokenStore store, ZaloOAuthClient oauth) {
        return new ZaloTokenManager(properties.getOaId(), store, oauth, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    ZaloOaClient zaloOaClient(ZaloConfigurationValidator validator, ZaloOaProperties properties,
                              ZaloTokenManager tokenManager) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloOaClient(tokenManager, httpClient, HttpZaloOaClient.DEFAULT_ENDPOINT,
                properties.getRequestTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    ZaloZbsClient zaloZbsClient(ZaloConfigurationValidator validator, ZaloOaProperties properties,
                                ZaloTokenManager tokenManager) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloZbsClient(tokenManager, httpClient, HttpZaloZbsClient.DEFAULT_ENDPOINT,
                properties.getRequestTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
    ZaloWebhookParser zaloWebhookParser(ZaloConfigurationValidator validator) {
        return new ZaloWebhookParser();
    }

    static final class ZaloConfigurationValidator {

        private ZaloConfigurationValidator(ZaloOaProperties properties,
                                           ObjectProvider<ZaloTokenManager> tokenManagers) {
            boolean webhookEnabled = properties.getWebhook().isEnabled();
            if (webhookEnabled && isBlank(properties.getOaSecretKey())) {
                throw new ZaloException("vietnam.zalo-oa.oa-secret-key is required when "
                        + "vietnam.zalo-oa.webhook.enabled=true");
            }
            if (properties.getAppId() == null) {
                if (webhookEnabled) {
                    throw new ZaloException("vietnam.zalo-oa.app-id is required when "
                            + "vietnam.zalo-oa.webhook.enabled=true");
                }
                return;
            }
            if (properties.getAppId().isBlank()) {
                throw new ZaloException("vietnam.zalo-oa.app-id must not be blank");
            }
            if (isBlank(properties.getSecretKey())) {
                throw new ZaloException("vietnam.zalo-oa.secret-key is required when Zalo OA "
                        + "auto-configuration is enabled");
            }
            if (isBlank(properties.getOaId()) && tokenManagers.getIfAvailable() == null) {
                throw new ZaloException("vietnam.zalo-oa.oa-id is required unless a ZaloTokenManager bean is provided");
            }
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
