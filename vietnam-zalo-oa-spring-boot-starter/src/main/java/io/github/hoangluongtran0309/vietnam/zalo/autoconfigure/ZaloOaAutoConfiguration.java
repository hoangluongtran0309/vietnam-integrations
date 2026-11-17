package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOaClient;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloZbsClient;
import io.github.hoangluongtran0309.vietnam.zalo.InMemoryZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.JdbcZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloAccountConfig;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOa;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenManager;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookRouter;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloZbsClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ZaloOaProperties.class)
public class ZaloOaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock zaloClock() {
        return Clock.systemUTC();
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
            return new JdbcZaloTokenStore(dataSource, properties.getJdbc().getLockTimeout());
        }
        return new InMemoryZaloTokenStore();
    }

    @Bean
    @ConditionalOnMissingBean
    ZaloOaRegistry zaloOaRegistry(ZaloOaProperties properties, ZaloTokenStore tokenStore, Clock clock) {
        validateTimeouts(properties);
        List<ZaloAccountConfig> accounts = accountConfigs(properties);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        try {
            return ZaloOaRegistry.builder(accounts, tokenStore)
                    .httpClient(httpClient)
                    .oauthEndpoint(HttpZaloOAuthClient.DEFAULT_ENDPOINT)
                    .messagingEndpoint(HttpZaloOaClient.DEFAULT_ENDPOINT)
                    .zbsEndpoint(HttpZaloZbsClient.DEFAULT_ENDPOINT)
                    .requestTimeout(properties.getRequestTimeout())
                    .lockTimeout(properties.getJdbc().getLockTimeout())
                    .clock(clock)
                    .defaultAccount(normalize(properties.getDefaultAccount()))
                    .build();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ZaloException("Invalid vietnam.zalo-oa account configuration: " + e.getMessage(), e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(RootApplicationCredentialsCondition.class)
    ZaloOAuthClient zaloOAuthClient(ZaloOaProperties properties, Clock clock) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new HttpZaloOAuthClient(properties.getAppId(), properties.getSecretKey(), httpClient,
                HttpZaloOAuthClient.DEFAULT_ENDPOINT, properties.getRequestTimeout(), clock);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "webhook.enabled", havingValue = "true")
    ZaloWebhookRouter zaloWebhookRouter(ZaloOaRegistry registry, ZaloOaProperties properties) {
        validateWebhookSecrets(properties);
        return new ZaloWebhookRouter(registry);
    }

    @Bean
    @ConditionalOnMissingBean(ZaloTokenManager.class)
    FactoryBean<ZaloTokenManager> zaloTokenManager(ZaloOaRegistry registry) {
        return new DefaultOaComponentFactoryBean<>(registry, ZaloOa::tokenManager, ZaloTokenManager.class);
    }

    @Bean
    @ConditionalOnMissingBean(ZaloOaClient.class)
    FactoryBean<ZaloOaClient> zaloOaClient(ZaloOaRegistry registry) {
        return new DefaultOaComponentFactoryBean<>(registry, ZaloOa::messaging, ZaloOaClient.class);
    }

    @Bean
    @ConditionalOnMissingBean(ZaloZbsClient.class)
    FactoryBean<ZaloZbsClient> zaloZbsClient(ZaloOaRegistry registry) {
        return new DefaultOaComponentFactoryBean<>(registry, ZaloOa::zbs, ZaloZbsClient.class);
    }

    private static List<ZaloAccountConfig> accountConfigs(ZaloOaProperties properties) {
        Map<String, ZaloOaProperties.Account> configuredAccounts = properties.getAccounts();
        boolean hasAccounts = configuredAccounts != null && !configuredAccounts.isEmpty();
        boolean hasLegacyOaId = !isBlank(properties.getOaId());

        if (hasAccounts && hasLegacyOaId) {
            throw new ZaloException("Configure either vietnam.zalo-oa.accounts or vietnam.zalo-oa.oa-id, not both");
        }
        if (!hasAccounts && !hasLegacyOaId) {
            throw new ZaloException("Configure an Official Account using either vietnam.zalo-oa.accounts or "
                    + "vietnam.zalo-oa.oa-id");
        }

        List<ZaloAccountConfig> result = new ArrayList<>();
        if (hasAccounts) {
            for (Map.Entry<String, ZaloOaProperties.Account> entry : configuredAccounts.entrySet()) {
                String alias = entry.getKey();
                ZaloOaProperties.Account account = entry.getValue();
                if (account == null) {
                    throw new ZaloException("vietnam.zalo-oa.accounts." + alias + " must define account settings");
                }
                String prefix = "vietnam.zalo-oa.accounts." + alias;
                result.add(newAccountConfig(
                        alias,
                        fallback(account.getAppId(), properties.getAppId(), prefix + ".app-id",
                                "vietnam.zalo-oa.app-id"),
                        fallback(account.getSecretKey(), properties.getSecretKey(), prefix + ".secret-key",
                                "vietnam.zalo-oa.secret-key"),
                        required(account.getOaId(), prefix + ".oa-id"),
                        normalize(account.getOaSecretKey())));
            }
        } else {
            result.add(newAccountConfig(
                    "default",
                    required(properties.getAppId(), "vietnam.zalo-oa.app-id"),
                    required(properties.getSecretKey(), "vietnam.zalo-oa.secret-key"),
                    properties.getOaId(),
                    normalize(properties.getOaSecretKey())));
        }

        String defaultAccount = normalize(properties.getDefaultAccount());
        if (defaultAccount != null && result.stream().noneMatch(account -> account.alias().equals(defaultAccount))) {
            throw new ZaloException("vietnam.zalo-oa.default-account references unknown alias '" + defaultAccount
                    + "'. Configured aliases: " + result.stream().map(ZaloAccountConfig::alias).toList());
        }
        return List.copyOf(result);
    }

    private static ZaloAccountConfig newAccountConfig(String alias, String appId, String secretKey,
                                                       String oaId, String oaSecretKey) {
        try {
            return new ZaloAccountConfig(alias, appId, secretKey, oaId, oaSecretKey);
        } catch (IllegalArgumentException e) {
            throw new ZaloException("Invalid vietnam.zalo-oa account '" + alias + "': " + e.getMessage(), e);
        }
    }

    private static void validateWebhookSecrets(ZaloOaProperties properties) {
        if (properties.getAccounts() != null && !properties.getAccounts().isEmpty()) {
            properties.getAccounts().forEach((alias, account) -> {
                if (account == null || isBlank(account.getOaSecretKey())) {
                    throw new ZaloException("vietnam.zalo-oa.accounts." + alias
                            + ".oa-secret-key is required when vietnam.zalo-oa.webhook.enabled=true");
                }
            });
        } else if (!isBlank(properties.getOaId()) && isBlank(properties.getOaSecretKey())) {
            throw new ZaloException("vietnam.zalo-oa.oa-secret-key is required when "
                    + "vietnam.zalo-oa.webhook.enabled=true");
        }
    }

    private static void validateTimeouts(ZaloOaProperties properties) {
        if (properties.getConnectTimeout() == null || properties.getConnectTimeout().isZero()
                || properties.getConnectTimeout().isNegative()) {
            throw new ZaloException("vietnam.zalo-oa.connect-timeout must be positive");
        }
        if (properties.getRequestTimeout() == null || properties.getRequestTimeout().isZero()
                || properties.getRequestTimeout().isNegative()) {
            throw new ZaloException("vietnam.zalo-oa.request-timeout must be positive");
        }
        if (properties.getJdbc() == null || properties.getJdbc().getLockTimeout() == null
                || properties.getJdbc().getLockTimeout().compareTo(properties.getRequestTimeout()) <= 0) {
            throw new ZaloException("vietnam.zalo-oa.jdbc.lock-timeout must be greater than "
                    + "vietnam.zalo-oa.request-timeout");
        }
    }

    private static String fallback(String value, String fallback, String propertyName, String fallbackPropertyName) {
        if (!isBlank(value)) {
            return value;
        }
        if (!isBlank(fallback)) {
            return fallback;
        }
        throw new ZaloException(propertyName + " or " + fallbackPropertyName + " is required");
    }

    private static String required(String value, String propertyName) {
        if (isBlank(value)) {
            throw new ZaloException(propertyName + " is required and must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static final class RootApplicationCredentialsCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ZaloOaProperties properties = Binder.get(context.getEnvironment())
                    .bind("vietnam.zalo-oa", ZaloOaProperties.class)
                    .orElseGet(ZaloOaProperties::new);
            return !isBlank(properties.getAppId()) && !isBlank(properties.getSecretKey());
        }
    }

    private static final class DefaultOaComponentFactoryBean<T> implements FactoryBean<T> {

        private final T component;
        private final Class<T> componentType;

        private DefaultOaComponentFactoryBean(ZaloOaRegistry registry, Function<ZaloOa, T> selector,
                                               Class<T> componentType) {
            this.component = registry.defaultOa().map(selector).orElse(null);
            this.componentType = componentType;
        }

        @Override
        public T getObject() {
            return component;
        }

        @Override
        public Class<?> getObjectType() {
            return component == null ? null : componentType;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }
}
