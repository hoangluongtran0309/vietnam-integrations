package io.github.hoangluongtran0309.vietnam.zalo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** PostgreSQL token store using a row lock to serialize single-use refresh token rotation across instances. */
public final class JdbcZaloTokenStore implements ZaloTokenStore {

    /** Default row-lock wait, longer than the default HTTP request timeout used while holding the lock. */
    public static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(15);

    private static final String LOCK_TIMEOUT_SQL = "SELECT set_config('lock_timeout', ?, true)";
    private static final String LOCK_NOT_AVAILABLE_SQL_STATE = "55P03";
    private static final String SELECT = "SELECT access_token, refresh_token, access_token_expires_at "
            + "FROM zalo_oa_tokens WHERE oa_id = ?";
    private static final String SELECT_FOR_UPDATE = SELECT + " FOR UPDATE";
    private static final String UPSERT = "INSERT INTO zalo_oa_tokens "
            + "(oa_id, access_token, refresh_token, access_token_expires_at, updated_at) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT (oa_id) DO UPDATE SET access_token = EXCLUDED.access_token, "
            + "refresh_token = EXCLUDED.refresh_token, access_token_expires_at = EXCLUDED.access_token_expires_at, "
            + "updated_at = EXCLUDED.updated_at";
    private static final String UPDATE = "UPDATE zalo_oa_tokens SET access_token = ?, refresh_token = ?, "
            + "access_token_expires_at = ?, updated_at = ? WHERE oa_id = ?";

    private final DataSource dataSource;
    private final String lockTimeoutSetting;
    private final Clock clock;

    public JdbcZaloTokenStore(DataSource dataSource) {
        this(dataSource, DEFAULT_LOCK_TIMEOUT, Clock.systemUTC());
    }

    /**
     * Creates a PostgreSQL token store with a bounded row-lock wait.
     *
     * <p>The lock timeout must be positive and must be longer than the HTTP request timeout used to refresh a
     * token. The instance holding the row lock may legitimately wait for Zalo until that request timeout expires.
     *
     * @param dataSource PostgreSQL data source
     * @param lockTimeout maximum time to wait for the token row lock
     */
    public JdbcZaloTokenStore(DataSource dataSource, Duration lockTimeout) {
        this(dataSource, lockTimeout, Clock.systemUTC());
    }

    public JdbcZaloTokenStore(DataSource dataSource, Clock clock) {
        this(dataSource, DEFAULT_LOCK_TIMEOUT, clock);
    }

    JdbcZaloTokenStore(DataSource dataSource, Duration lockTimeout, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        Duration validatedLockTimeout = requirePositive(lockTimeout);
        this.lockTimeoutSetting = Math.max(1, validatedLockTimeout.toMillis()) + "ms";
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<ZaloToken> load(String oaId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT)) {
            statement.setString(1, oaId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ZaloException("Cannot load Zalo token from PostgreSQL", e);
        }
    }

    @Override
    public void save(String oaId, ZaloToken token) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setString(1, oaId);
            write(statement, 2, token);
            statement.setTimestamp(5, Timestamp.from(clock.instant()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new ZaloException("Cannot save Zalo token to PostgreSQL", e);
        }
    }

    @Override
    public ZaloToken updateAtomically(String oaId, UnaryOperator<ZaloToken> updater) {
        Objects.requireNonNull(updater, "updater");
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Exception failure = null;
            try {
                connection.setAutoCommit(false);
                setLockTimeout(connection);
                ZaloToken current;
                try (PreparedStatement select = connection.prepareStatement(SELECT_FOR_UPDATE)) {
                    select.setString(1, oaId);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) {
                            throw new ZaloException("No token stored for OA " + oaId
                                    + ". Complete the OAuth authorization flow first.");
                        }
                        current = read(result);
                    }
                }
                ZaloToken updated = Objects.requireNonNull(updater.apply(current), "updated token");
                try (PreparedStatement update = connection.prepareStatement(UPDATE)) {
                    write(update, 1, updated);
                    update.setTimestamp(4, Timestamp.from(clock.instant()));
                    update.setString(5, oaId);
                    if (update.executeUpdate() != 1) {
                        throw new ZaloException("Token row disappeared during atomic update for OA " + oaId);
                    }
                }
                connection.commit();
                return updated;
            } catch (RuntimeException | SQLException e) {
                failure = e;
                rollback(connection, e);
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (e instanceof SQLException sqlException
                        && LOCK_NOT_AVAILABLE_SQL_STATE.equals(sqlException.getSQLState())) {
                    throw new ZaloException("Timed out because another instance is refreshing the Zalo token for OA "
                            + oaId, e);
                }
                throw new ZaloException("Cannot update Zalo token atomically in PostgreSQL", e);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit, failure);
            }
        } catch (SQLException e) {
            throw new ZaloException("Cannot open PostgreSQL transaction for Zalo token", e);
        }
    }

    private void setLockTimeout(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOCK_TIMEOUT_SQL)) {
            statement.setString(1, lockTimeoutSetting);
            statement.execute();
        }
    }

    private static ZaloToken read(ResultSet result) throws SQLException {
        return new ZaloToken(result.getString("access_token"), result.getString("refresh_token"),
                result.getTimestamp("access_token_expires_at").toInstant());
    }

    private static void write(PreparedStatement statement, int offset, ZaloToken token) throws SQLException {
        statement.setString(offset, token.accessToken());
        statement.setString(offset + 1, token.refreshToken());
        statement.setTimestamp(offset + 2, Timestamp.from(token.accessTokenExpiresAt()));
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean originalAutoCommit, Exception original) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException restore) {
            if (original != null) {
                original.addSuppressed(restore);
            } else {
                throw new ZaloException("Cannot restore auto-commit after updating the Zalo token", restore);
            }
        }
    }

    private static Duration requirePositive(Duration lockTimeout) {
        Objects.requireNonNull(lockTimeout, "lockTimeout");
        if (lockTimeout.isZero() || lockTimeout.isNegative()) {
            throw new IllegalArgumentException("lockTimeout must be positive");
        }
        return lockTimeout;
    }
}
