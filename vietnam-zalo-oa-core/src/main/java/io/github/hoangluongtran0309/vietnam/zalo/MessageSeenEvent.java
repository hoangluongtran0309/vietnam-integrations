package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

public record MessageSeenEvent(String appId, String oaId, String senderId, String recipientId, Instant occurredAt,
                               String messageId, Instant seenAt) implements ZaloWebhookEvent {
    @Override public String eventName() { return "user_seen_message"; }
}
