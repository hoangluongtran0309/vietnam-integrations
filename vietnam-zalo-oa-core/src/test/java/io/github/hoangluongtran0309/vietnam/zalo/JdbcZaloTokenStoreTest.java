package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class JdbcZaloTokenStoreTest {

    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private DataSource dataSource;

    @BeforeEach
    void createSchema() throws SQLException, IOException {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        dataSource = source;
        String schema;
        try (var in = getClass().getResourceAsStream(
                "/io/github/hoangluongtran0309/vietnam/zalo/postgresql/V1__create_zalo_oa_tokens.sql")) {
            schema = new String(java.util.Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS zalo_oa_tokens");
            statement.execute(schema);
        }
    }

    @Test
    void serializesRefreshAcrossTwoStoreInstances() throws Exception {
        JdbcZaloTokenStore firstStore = store();
        JdbcZaloTokenStore secondStore = store();
        firstStore.save("oa", new ZaloToken("old", "refresh-old", NOW.plusSeconds(30)));
        AtomicInteger refreshCalls = new AtomicInteger();
        ZaloOAuthClient oauth = new ZaloOAuthClient() {
            @Override public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
                throw new UnsupportedOperationException();
            }
            @Override public ZaloToken refresh(String refreshToken) {
                refreshCalls.incrementAndGet();
                return new ZaloToken("new", "refresh-new", NOW.plus(Duration.ofHours(1)));
            }
        };
        ZaloTokenManager first = manager(firstStore, oauth);
        ZaloTokenManager second = manager(secondStore, oauth);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var one = executor.submit(() -> { start.await(); return first.accessToken(); });
            var two = executor.submit(() -> { start.await(); return second.accessToken(); });
            start.countDown();
            assertThat(one.get(10, TimeUnit.SECONDS)).isEqualTo("new");
            assertThat(two.get(10, TimeUnit.SECONDS)).isEqualTo("new");
        } finally {
            executor.shutdownNow();
        }
        assertThat(refreshCalls).hasValue(1);
        assertThat(firstStore.load("oa")).get().extracting(ZaloToken::refreshToken).isEqualTo("refresh-new");
    }

    @Test
    void rollsBackWhenUpdaterFails() {
        JdbcZaloTokenStore store = store();
        ZaloToken original = new ZaloToken("old", "refresh-old", NOW.plusSeconds(30));
        store.save("oa", original);

        assertThatThrownBy(() -> store.updateAtomically("oa", ignored -> {
            throw new ZaloException("remote refresh failed");
        })).isInstanceOf(ZaloException.class).hasMessageContaining("remote refresh failed");
        assertThat(store.load("oa")).contains(original);
    }

    private JdbcZaloTokenStore store() {
        return new JdbcZaloTokenStore(dataSource, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ZaloTokenManager manager(ZaloTokenStore store, ZaloOAuthClient oauth) {
        return new ZaloTokenManager("oa", store, oauth, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
