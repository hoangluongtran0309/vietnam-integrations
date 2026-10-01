package io.github.hoangluongtran0309.vietnam.zalo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies the signature Zalo sends with OA webhook events.
 *
 * <p>TODO(v0.2): confirm the exact formula and header name against current Zalo OA documentation and add
 * a test with a real captured event. Current assumption: {@code mac=sha256(appId + body + timestamp + oaSecretKey)}.
 */
public class ZaloWebhookVerifier {

    private final String appId;
    private final String oaSecretKey;

    public ZaloWebhookVerifier(String appId, String oaSecretKey) {
        this.appId = appId;
        this.oaSecretKey = oaSecretKey;
    }

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

    static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
