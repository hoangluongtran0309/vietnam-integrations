package io.github.hoangluongtran0309.vietnam.zalo;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

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
        this.appId = requireNotBlank(appId, "appId");
        this.oaSecretKey = requireNotBlank(oaSecretKey, "oaSecretKey");
    }

    public boolean verify(String rawBody, String signatureHeader) {
        Objects.requireNonNull(rawBody, "rawBody");
        return verify(rawBody.getBytes(StandardCharsets.UTF_8), signatureHeader);
    }

    /** Verifies the signature against the exact request body bytes after strict UTF-8 validation. */
    public boolean verify(byte[] rawBody, String signatureHeader) {
        String decodedBody = decodeUtf8(rawBody);
        String timestamp = timestamp(decodedBody);
        return verify(decodedBody, timestamp, signatureHeader);
    }

    /** Verifies a body when its timestamp has already been parsed from the exact same raw body. */
    public boolean verify(String rawBody, String timestamp, String signatureHeader) {
        if (signatureHeader == null) {
            return false;
        }
        String normalized = signatureHeader.toLowerCase(Locale.ROOT);
        String provided = normalized.startsWith("mac=") ? normalized.substring(4) : normalized;
        String expected = sha256Hex(appId + rawBody + timestamp + oaSecretKey);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII));
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

    private static String requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
