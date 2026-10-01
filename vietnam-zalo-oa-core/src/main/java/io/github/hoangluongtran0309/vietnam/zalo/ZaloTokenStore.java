package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Optional;

/**
 * Port for persisting OA tokens. The default in-memory implementation is only safe for a single instance;
 * multi-instance deployments need a shared store (JDBC/Redis) — see docs/adr/0004-zalo-token-store.md.
 */
public interface ZaloTokenStore {

    Optional<ZaloToken> load(String oaId);

    void save(String oaId, ZaloToken token);
}
