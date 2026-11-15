package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.Clock;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "app-id")
@EnableConfigurationProperties(ZaloOaProperties.class)
public class ZaloOaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZaloConfigurationMarker zaloConfigurationMarker(ZaloOaProperties properties) {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new ZaloException("vietnam.zalo-oa.secret-key is required when Zalo OA auto-configuration is enabled");
        }
        return new ZaloConfigurationMarker();
    }

    @Bean
    @ConditionalOnMissingBean
    ZaloTokenStore zaloTokenStore(ZaloOaProperties properties, ObjectProvider<DataSource> dataSources) {
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
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "secret-key")
    ZaloOAuthClient zaloOAuthClient(ZaloOaProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloOAuthClient(properties.getAppId(), properties.getSecretKey(), httpClient,
                HttpZaloOAuthClient.DEFAULT_ENDPOINT, properties.getRequestTimeout(), Clock.systemUTC());
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
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "oa-id")
    ZaloTokenManager zaloTokenManager(ZaloOaProperties properties, ZaloTokenStore store, ZaloOAuthClient oauth) {
        return new ZaloTokenManager(properties.getOaId(), store, oauth, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZaloTokenManager.class)
    ZaloOaClient zaloOaClient(ZaloOaProperties properties, ZaloTokenManager tokenManager) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloOaClient(tokenManager, httpClient, HttpZaloOaClient.DEFAULT_ENDPOINT,
                properties.getRequestTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZaloTokenManager.class)
    ZaloZbsClient zaloZbsClient(ZaloOaProperties properties, ZaloTokenManager tokenManager) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloZbsClient(tokenManager, httpClient, HttpZaloZbsClient.DEFAULT_ENDPOINT,
                properties.getRequestTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    ZaloWebhookParser zaloWebhookParser() {
        return new ZaloWebhookParser();
    }

    static final class ZaloConfigurationMarker {
    }
}
