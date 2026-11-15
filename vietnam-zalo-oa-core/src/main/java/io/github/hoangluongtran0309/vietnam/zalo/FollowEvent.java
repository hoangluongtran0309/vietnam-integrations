package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

public record FollowEvent(String appId, String senderId, String recipientId, Instant occurredAt)
        implements ZaloWebhookEvent {
    @Override public String eventName() { return "follow"; }
}
