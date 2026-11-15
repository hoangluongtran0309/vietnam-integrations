package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;
import java.util.Objects;

/** Result returned after Zalo accepts a message. */
public record ZaloMessageResult(String messageId, String recipient, Instant sentAt) {
    public ZaloMessageResult {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(sentAt, "sentAt");
    }
}
