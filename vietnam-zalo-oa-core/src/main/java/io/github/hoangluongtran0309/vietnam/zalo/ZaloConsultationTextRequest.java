package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Objects;

/** Request for a plain or quoted OA consultation text message. */
public record ZaloConsultationTextRequest(String userId, String text, String quoteMessageId) {

    public ZaloConsultationTextRequest {
        requireText(userId, "userId");
        requireText(text, "text");
        // TODO(v0.2): verify whether Zalo counts code points or UTF-16 units — capture in WP-2D.1.
        if (text.length() > 2_000) {
            throw new IllegalArgumentException("text must not exceed 2000 characters");
        }
        if (quoteMessageId != null && quoteMessageId.isBlank()) {
            throw new IllegalArgumentException("quoteMessageId must be null or non-blank");
        }
    }

    public ZaloConsultationTextRequest(String userId, String text) {
        this(userId, text, null);
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
