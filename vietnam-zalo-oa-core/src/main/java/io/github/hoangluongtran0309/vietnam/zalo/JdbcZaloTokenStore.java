package io.github.hoangluongtran0309.vietnam.zalo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** PostgreSQL token store using a row lock to serialize single-use refresh token rotation across instances. */
public final class JdbcZaloTokenStore implements ZaloTokenStore {

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
    private final Clock clock;

    public JdbcZaloTokenStore(DataSource dataSource) {
        this(dataSource, Clock.systemUTC());
    }

    public JdbcZaloTokenStore(DataSource dataSource, Clock clock) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
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
            connection.setAutoCommit(false);
            try {
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
                rollback(connection, e);
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new ZaloException("Cannot update Zalo token atomically in PostgreSQL", e);
            }
        } catch (SQLException e) {
            throw new ZaloException("Cannot open PostgreSQL transaction for Zalo token", e);
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
}
