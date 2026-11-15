package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

public record MessageDeliveredEvent(String appId, String senderId, String recipientId, Instant occurredAt,
                                    String messageId, String trackingId, Instant deliveredAt)
        implements ZaloWebhookEvent {
    @Override public String eventName() { return "user_received_message"; }
}
