package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    void rejectsNonPositiveLockTimeout() {
        assertThatThrownBy(() -> new JdbcZaloTokenStore(dataSource, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> new JdbcZaloTokenStore(dataSource, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void failsWithoutCreatingRowWhenTokenDoesNotExist() {
        JdbcZaloTokenStore store = store();

        assertThatThrownBy(() -> store.updateAtomically("missing", token -> token))
                .isInstanceOf(ZaloException.class)
                .hasMessageContaining("OAuth authorization flow");
        assertThat(store.load("missing")).isEmpty();
    }

    @Test
    void timesOutDeterministicallyWhenAnotherInstanceHoldsRowLock() throws Exception {
        JdbcZaloTokenStore firstStore = store(Duration.ofSeconds(5));
        JdbcZaloTokenStore secondStore = store(Duration.ofMillis(300));
        firstStore.save("oa", new ZaloToken("old", "refresh-old", NOW.plusSeconds(30)));
        CountDownLatch updaterEntered = new CountDownLatch(1);
        CountDownLatch releaseUpdater = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var firstUpdate = executor.submit(() -> firstStore.updateAtomically("oa", ignored -> {
                updaterEntered.countDown();
                await(releaseUpdater);
                return new ZaloToken("new", "refresh-new", NOW.plus(Duration.ofHours(1)));
            }));
            assertThat(updaterEntered.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> secondStore.updateAtomically("oa", token -> token))
                    .isInstanceOfSatisfying(ZaloException.class, exception -> {
                        assertThat(exception).hasMessageContaining("another instance is refreshing the Zalo token");
                        assertThat(exception.getCause()).isInstanceOfSatisfying(SQLException.class,
                                sqlException -> assertThat(sqlException.getSQLState()).isEqualTo("55P03"));
                    });

            releaseUpdater.countDown();
            assertThat(firstUpdate.get(10, TimeUnit.SECONDS).accessToken()).isEqualTo("new");
        } finally {
            releaseUpdater.countDown();
            executor.shutdownNow();
        }
        assertThat(firstStore.load("oa")).get().extracting(ZaloToken::refreshToken).isEqualTo("refresh-new");
    }

    @Test
    void rollsBackNewTokenWhenDatabaseUpdateFails() {
        JdbcZaloTokenStore normalStore = store();
        ZaloToken original = new ZaloToken("old", "refresh-old", NOW.plusSeconds(30));
        normalStore.save("oa", original);
        JdbcZaloTokenStore failingStore = new JdbcZaloTokenStore(failTokenUpdates(dataSource),
                Duration.ofSeconds(1), Clock.fixed(NOW, ZoneOffset.UTC));
        AtomicBoolean updaterReturnedNewToken = new AtomicBoolean();

        assertThatThrownBy(() -> failingStore.updateAtomically("oa", ignored -> {
            updaterReturnedNewToken.set(true);
            return new ZaloToken("new", "refresh-new", NOW.plus(Duration.ofHours(1)));
        })).isInstanceOf(ZaloException.class).hasMessageContaining("atomically");

        assertThat(updaterReturnedNewToken).isTrue();
        assertThat(normalStore.load("oa")).contains(original);
    }

    @Test
    void restoresOriginalAutoCommitAfterAtomicUpdate() throws SQLException {
        JdbcZaloTokenStore normalStore = store();
        normalStore.save("oa", new ZaloToken("old", "refresh-old", NOW.plusSeconds(30)));
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            JdbcZaloTokenStore store = new JdbcZaloTokenStore(nonClosing(connection), Duration.ofSeconds(1),
                    Clock.fixed(NOW, ZoneOffset.UTC));

            store.updateAtomically("oa", token -> token);

            assertThat(connection.getAutoCommit()).isTrue();
        }
    }

    private JdbcZaloTokenStore store() {
        return store(JdbcZaloTokenStore.DEFAULT_LOCK_TIMEOUT);
    }

    private JdbcZaloTokenStore store(Duration lockTimeout) {
        return new JdbcZaloTokenStore(dataSource, lockTimeout, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ZaloTokenManager manager(ZaloTokenStore store, ZaloOAuthClient oauth) {
        return new ZaloTokenManager("oa", store, oauth, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for test coordination latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for test coordination latch", e);
        }
    }

    private static DataSource failTokenUpdates(DataSource delegate) {
        return proxy(DataSource.class, (proxy, method, args) -> {
            Object result = invoke(method, delegate, args);
            if (result instanceof Connection connection && method.getName().startsWith("getConnection")) {
                return failTokenUpdates(connection);
            }
            return result;
        });
    }

    private static Connection failTokenUpdates(Connection delegate) {
        return proxy(Connection.class, (proxy, method, args) -> {
            Object result = invoke(method, delegate, args);
            if (result instanceof PreparedStatement statement && method.getName().equals("prepareStatement")
                    && args[0] instanceof String sql && sql.startsWith("UPDATE zalo_oa_tokens SET")) {
                return proxy(PreparedStatement.class, (statementProxy, statementMethod, statementArgs) -> {
                    if (statementMethod.getName().equals("executeUpdate")) {
                        throw new SQLException("Simulated token update failure", "XX000");
                    }
                    return invoke(statementMethod, statement, statementArgs);
                });
            }
            return result;
        });
    }

    private static DataSource nonClosing(Connection connection) {
        Connection nonClosingConnection = proxy(Connection.class, (proxy, method, args) -> {
            if (method.getName().equals("close")) {
                return null;
            }
            return invoke(method, connection, args);
        });
        return proxy(DataSource.class, (proxy, method, args) -> {
            if (method.getName().startsWith("getConnection")) {
                return nonClosingConnection;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
