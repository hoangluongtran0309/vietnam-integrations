package io.github.hoangluongtran0309.vietnam.zalo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Verifies the signature Zalo sends with OA webhook events.
 *
 * The {@code X-ZEvent-Signature} value is {@code mac=sha256(appId + rawBody + timestamp + oaSecretKey)}.
 */
public class ZaloWebhookVerifier {

    private final String appId;
    private final String oaSecretKey;

    public ZaloWebhookVerifier(String appId, String oaSecretKey) {
        this.appId = appId;
        this.oaSecretKey = oaSecretKey;
    }

    public boolean verify(String rawBody, String signatureHeader) {
        String timestamp = timestamp(rawBody);
        return verify(rawBody, timestamp, signatureHeader);
    }

    /** Verifies a body when its timestamp has already been parsed from the exact same raw body. */
    public boolean verify(String rawBody, String timestamp, String signatureHeader) {
        if (signatureHeader == null) {
            return false;
        }
        String provided = signatureHeader.startsWith("mac=") ? signatureHeader.substring(4) : signatureHeader;
        String expected = sha256Hex(appId + rawBody + timestamp + oaSecretKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    public String timestamp(String rawBody) {
        try {
            JsonElement parsed = JsonParser.parseString(rawBody);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Webhook body must be a JSON object");
            }
            JsonObject object = parsed.getAsJsonObject();
            JsonElement value = object.get("timestamp");
            if (value == null || value.isJsonNull() || value.getAsString().isBlank()) {
                throw new IllegalArgumentException("Webhook body is missing timestamp");
            }
            return value.getAsString();
        } catch (com.google.gson.JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("Webhook body is not valid JSON", e);
        }
    }

    static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
