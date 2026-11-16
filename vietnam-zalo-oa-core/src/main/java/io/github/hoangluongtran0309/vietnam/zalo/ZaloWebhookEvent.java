package io.github.hoangluongtran0309.vietnam.zalo;

import java.time.Instant;

/** Framework-neutral webhook event. Spring publishes these records directly as application events. */
public sealed interface ZaloWebhookEvent permits UserTextMessageEvent, FollowEvent, UnfollowEvent,
        MessageDeliveredEvent, MessageSeenEvent, UnknownZaloWebhookEvent {

    String eventName();
    String appId();
    String oaId();
    String senderId();
    String recipientId();
    Instant occurredAt();
}
