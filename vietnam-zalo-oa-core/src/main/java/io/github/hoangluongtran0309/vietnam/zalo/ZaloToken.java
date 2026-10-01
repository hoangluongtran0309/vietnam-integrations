package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * OA access/refresh token pair. Zalo refresh tokens are single-use: every refresh returns a new pair
 * that MUST be persisted before the old one is discarded.
 */
public record ZaloToken(String accessToken, String refreshToken, Instant accessTokenExpiresAt) {

    public ZaloToken {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
    }

    public boolean isAccessTokenExpired(Instant now, Duration skew) {
        return !now.plus(skew).isBefore(accessTokenExpiresAt);
    }
}
