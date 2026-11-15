package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.regex.Pattern;

/** Configuration required to create one Official Account client set. */
public record ZaloAccountConfig(
        String alias,
        String appId,
        String secretKey,
        String oaId,
        String oaSecretKey) {

    private static final Pattern ALIAS = Pattern.compile("[a-z0-9-]{1,32}");

    public ZaloAccountConfig {
        if (alias == null || !ALIAS.matcher(alias).matches()) {
            throw new IllegalArgumentException("alias must match [a-z0-9-]{1,32}");
        }
        requireNotBlank(appId, "appId");
        requireNotBlank(secretKey, "secretKey");
        requireNotBlank(oaId, "oaId");
        if (oaSecretKey != null && oaSecretKey.isBlank()) {
            throw new IllegalArgumentException("oaSecretKey must be null or non-blank");
        }
    }

    @Override
    public String toString() {
        return "ZaloAccountConfig[alias=" + alias + ", appId=" + appId
                + ", secretKey=" + ZaloRedaction.secret(secretKey) + ", oaId=" + oaId
                + ", oaSecretKey=" + ZaloRedaction.secret(oaSecretKey) + "]";
    }

    private static void requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
