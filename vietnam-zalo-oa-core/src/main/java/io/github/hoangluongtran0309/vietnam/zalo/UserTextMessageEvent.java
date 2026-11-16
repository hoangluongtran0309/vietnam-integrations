package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

public record UserTextMessageEvent(String appId, String oaId, String senderId, String recipientId, Instant occurredAt,
                                   String messageId, String text) implements ZaloWebhookEvent {
    @Override public String eventName() { return "user_send_text"; }
}
