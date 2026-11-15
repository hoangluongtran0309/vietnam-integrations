package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Clock;
import java.time.Duration;

/**
 * Returns a valid access token, refreshing at most once concurrently (single-flight within this JVM).
 */
public class ZaloTokenManager {

    private static final Duration SKEW = Duration.ofMinutes(5);

    private final String oaId;
    private final ZaloTokenStore store;
    private final ZaloOAuthClient oauthClient;
    private final Clock clock;

    public ZaloTokenManager(String oaId, ZaloTokenStore store, ZaloOAuthClient oauthClient, Clock clock) {
        this.oaId = oaId;
        this.store = store;
        this.oauthClient = oauthClient;
        this.clock = clock;
    }

    public String accessToken() {
        ZaloToken current = loadOrFail();
        if (!current.isAccessTokenExpired(clock.instant(), SKEW)) {
            return current.accessToken();
        }
        return store.updateAtomically(oaId, locked -> {
            if (!locked.isAccessTokenExpired(clock.instant(), SKEW)) {
                return locked;
            }
            return oauthClient.refresh(locked.refreshToken());
        }).accessToken();
    }

    private ZaloToken loadOrFail() {
        return store.load(oaId).orElseThrow(() -> new ZaloException(
                "No token stored for OA " + oaId + ". Complete the OAuth authorization flow first."));
    }
}
