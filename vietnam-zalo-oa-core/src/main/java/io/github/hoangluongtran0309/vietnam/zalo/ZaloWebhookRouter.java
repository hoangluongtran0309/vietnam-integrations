package io.github.hoangluongtran0309.vietnam.zalo;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/** Selects an Official Account and authenticates its webhook before parsing the typed event. */
public final class ZaloWebhookRouter {

    private final ZaloOaRegistry registry;
    private final ZaloWebhookParser parser;

    public ZaloWebhookRouter(ZaloOaRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        for (ZaloOa account : registry.accounts()) {
            if (registry.accountConfig(account.alias()).oaSecretKey() == null) {
                throw new IllegalArgumentException("Webhook secret is required for Zalo account alias '"
                        + account.alias() + "'");
            }
        }
        this.parser = new ZaloWebhookParser();
    }

    /** Routes and authenticates the exact request body bytes. */
    public ZaloWebhookEvent route(byte[] rawBody, String signatureHeader) {
        String decodedBody = decodeUtf8(rawBody);
        WebhookEnvelope envelope = envelope(decodedBody);
        ZaloOa account = resolveAccount(envelope);
        ZaloAccountConfig config = registry.accountConfig(account.alias());

        if (!account.appId().equals(envelope.appId())) {
            throw rejected("Webhook app_id does not match the routed Official Account");
        }
        if (config.oaSecretKey() == null) {
            throw rejected("Webhook secret is not configured for the routed Official Account");
        }
        ZaloWebhookVerifier verifier = new ZaloWebhookVerifier(account.appId(), config.oaSecretKey());
        if (!verifier.verify(decodedBody, verifier.timestamp(decodedBody), signatureHeader)) {
            throw rejected("Webhook signature is invalid");
        }
        return parser.parse(decodedBody, account.oaId());
    }

    private ZaloOa resolveAccount(WebhookEnvelope envelope) {
        if (envelope.oaId() != null) {
            return registry.findByOaId(envelope.oaId())
                    .orElseThrow(() -> rejected("Webhook Official Account is not configured"));
        }

        // TODO(v0.2): verify OA id nằm ở sender hay recipient cho từng event, và OA Secret Key là theo OA hay
        // theo App — xác nhận bằng capture thật ở WP-2D.1.
        Map<String, ZaloOa> matches = new LinkedHashMap<>();
        addMatch(matches, envelope.senderId());
        addMatch(matches, envelope.recipientId());
        if (matches.size() != 1) {
            throw rejected("Webhook must match exactly one configured Official Account");
        }
        return matches.values().iterator().next();
    }

    private void addMatch(Map<String, ZaloOa> matches, String candidateOaId) {
        if (candidateOaId != null) {
            registry.findByOaId(candidateOaId).ifPresent(account -> matches.put(account.oaId(), account));
        }
    }

    private static WebhookEnvelope envelope(String rawBody) {
        try {
            JsonElement parsed = JsonParser.parseString(rawBody);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Webhook body must be a JSON object");
            }
            JsonObject root = parsed.getAsJsonObject();
            validateTimestamp(required(root, "timestamp"));
            return new WebhookEnvelope(
                    required(root, "app_id"), optional(root, "oa_id"),
                    nested(root, "sender", "id"), nested(root, "recipient", "id"));
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("Webhook body is not valid JSON", e);
        }
    }

    private static String nested(JsonObject root, String objectName, String fieldName) {
        JsonElement element = root.get(objectName);
        return element != null && element.isJsonObject() ? optional(element.getAsJsonObject(), fieldName) : null;
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

    private static void validateTimestamp(String timestamp) {
        try {
            Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Webhook timestamp is not epoch milliseconds", e);
        }
    }

    private static String decodeUtf8(byte[] rawBody) {
        Objects.requireNonNull(rawBody, "rawBody");
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(rawBody))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Webhook body is not valid UTF-8", e);
        }
    }

    private static ZaloWebhookRejectedException rejected(String message) {
        return new ZaloWebhookRejectedException(message);
    }

    private record WebhookEnvelope(String appId, String oaId, String senderId, String recipientId) {
    }
}
