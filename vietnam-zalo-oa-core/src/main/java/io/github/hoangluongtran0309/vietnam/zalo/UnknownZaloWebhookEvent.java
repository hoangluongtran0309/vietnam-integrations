package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;
import java.util.Objects;

/** Forward-compatible event for webhook names not modelled by this library version. */
public record UnknownZaloWebhookEvent(String eventName, String appId, String oaId, String senderId, String recipientId,
                                     Instant occurredAt, String rawBody) implements ZaloWebhookEvent {
    public UnknownZaloWebhookEvent {
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(rawBody, "rawBody");
    }
}
