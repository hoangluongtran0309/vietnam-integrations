package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.HttpZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.autoconfigure.ZaloOaProperties;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
class ZaloDemoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", havingValue = "true")
    ZaloDemoOAuthClients zaloDemoOAuthClients(ZaloOaProperties properties,
                                               ObjectProvider<ZaloOAuthClient> rootOAuthClient) {
        Map<String, ZaloOAuthClient> clients = new LinkedHashMap<>();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        ZaloOAuthClient rootClient = rootOAuthClient.getIfAvailable();

        if (properties.getAccounts().isEmpty()) {
            clients.put("default", Objects.requireNonNull(rootClient,
                    "A ZaloOAuthClient is required for the legacy single-account demo"));
        } else {
            Map<String, ZaloOAuthClient> clientsByCredentials = new LinkedHashMap<>();
            properties.getAccounts().forEach((alias, account) -> {
                String appId = fallback(account.getAppId(), properties.getAppId());
                String secretKey = fallback(account.getSecretKey(), properties.getSecretKey());
                String credentialsKey = appId + '\0' + secretKey;
                ZaloOAuthClient client;
                if (rootClient != null && Objects.equals(appId, properties.getAppId())
                        && Objects.equals(secretKey, properties.getSecretKey())) {
                    client = rootClient;
                } else {
                    client = clientsByCredentials.computeIfAbsent(credentialsKey, ignored ->
                            new HttpZaloOAuthClient(appId, secretKey, httpClient,
                                    HttpZaloOAuthClient.DEFAULT_ENDPOINT, properties.getRequestTimeout(),
                                    Clock.systemUTC()));
                }
                clients.put(alias, client);
            });
        }
        return new ZaloDemoOAuthClients(clients);
    }

    @Bean
    @ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "token-store", havingValue = "jdbc")
    DataSource zaloDemoDataSource(
            @Value("${demo.zalo.jdbc-url}") String jdbcUrl,
            @Value("${demo.zalo.jdbc-user}") String user,
            @Value("${demo.zalo.jdbc-password}") String password) {
        if (jdbcUrl.isBlank()) {
            throw new IllegalStateException("ZALO_JDBC_URL is required when ZALO_TOKEN_STORE=jdbc");
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
