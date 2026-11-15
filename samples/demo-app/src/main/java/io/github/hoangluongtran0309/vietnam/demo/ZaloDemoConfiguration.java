package io.github.hoangluongtran0309.vietnam.demo;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
class ZaloDemoConfiguration {

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
}
