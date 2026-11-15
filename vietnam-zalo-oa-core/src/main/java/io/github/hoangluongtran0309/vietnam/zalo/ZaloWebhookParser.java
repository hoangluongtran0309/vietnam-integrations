package io.github.hoangluongtran0309.vietnam.zalo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.time.Instant;

/** Parses verified OA webhook payloads into the stable event subset supported by this release. */
public final class ZaloWebhookParser {

    public ZaloWebhookEvent parse(String rawBody) {
        try {
            JsonElement parsed = JsonParser.parseString(rawBody);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Webhook body must be a JSON object");
            }
            JsonObject root = parsed.getAsJsonObject();
            String eventName = required(root, "event_name");
            String appId = required(root, "app_id");
            String sender = nested(root, "sender", "id");
            String recipient = nested(root, "recipient", "id");
            Instant occurredAt = epoch(required(root, "timestamp"), "timestamp");
            return switch (eventName) {
                case "user_send_text" -> {
                    JsonObject message = object(root, "message");
                    yield new UserTextMessageEvent(appId, sender, recipient, occurredAt,
                            required(message, "msg_id"), required(message, "text"));
                }
                case "follow" -> new FollowEvent(appId, sender != null ? sender : nested(root, "follower", "id"),
                        recipient, occurredAt);
                case "unfollow" -> new UnfollowEvent(appId,
                        sender != null ? sender : nested(root, "follower", "id"), recipient, occurredAt);
                case "user_received_message" -> {
                    JsonObject message = object(root, "message");
                    String delivered = optional(message, "delivery_time");
                    yield new MessageDeliveredEvent(appId, sender, recipient, occurredAt,
                            required(message, "msg_id"), optional(message, "tracking_id"),
                            delivered == null ? occurredAt : epoch(delivered, "delivery_time"));
                }
                case "user_seen_message" -> {
                    JsonObject message = object(root, "message");
                    String seen = optional(message, "seen_time");
                    yield new MessageSeenEvent(appId, sender, recipient, occurredAt,
                            required(message, "msg_id"), seen == null ? occurredAt : epoch(seen, "seen_time"));
                }
                default -> new UnknownZaloWebhookEvent(eventName, appId, sender, recipient, occurredAt, rawBody);
            };
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("Webhook body is not valid JSON", e);
        }
    }

    private static JsonObject object(JsonObject root, String name) {
        JsonElement element = root.get(name);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Webhook body is missing object " + name);
        }
        return element.getAsJsonObject();
    }

    private static String nested(JsonObject root, String object, String name) {
        JsonElement element = root.get(object);
        return element != null && element.isJsonObject() ? optional(element.getAsJsonObject(), name) : null;
    }

    private static String required(JsonObject object, String name) {
        String value = optional(object, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Webhook body is missing " + name);
        }
        return value;
    }

    private static String optional(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Instant epoch(String value, String name) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Webhook " + name + " is not epoch milliseconds", e);
        }
    }
}
