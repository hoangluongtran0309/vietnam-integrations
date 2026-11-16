package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

public record UnfollowEvent(String appId, String oaId, String senderId, String recipientId, Instant occurredAt)
        implements ZaloWebhookEvent {
    @Override public String eventName() { return "unfollow"; }
}
