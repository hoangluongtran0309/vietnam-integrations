package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Port for persisting OA tokens. The default in-memory implementation is only safe for a single instance;
 * multi-instance deployments need a shared store (JDBC/Redis) — see docs/adr/0004-zalo-token-store.md.
 */
public interface ZaloTokenStore {

    Optional<ZaloToken> load(String oaId);

    void save(String oaId, ZaloToken token);

    /**
     * Atomically loads, updates and persists one OA token. Implementations must serialize this operation across all
     * callers sharing the store. The updater may perform the remote refresh call and must never be invoked twice by
     * the store.
     */
    ZaloToken updateAtomically(String oaId, UnaryOperator<ZaloToken> updater);
}
