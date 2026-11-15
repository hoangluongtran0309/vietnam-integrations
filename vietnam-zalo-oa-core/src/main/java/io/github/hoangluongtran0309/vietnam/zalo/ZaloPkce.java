package io.github.hoangluongtran0309.vietnam.zalo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** PKCE values for one OAuth authorization request. Never reuse a verifier. */
public record ZaloPkce(String codeVerifier, String codeChallenge, String state) {

    private static final char[] UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final char[] ALL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    public ZaloPkce {
        Objects.requireNonNull(codeVerifier, "codeVerifier");
        Objects.requireNonNull(codeChallenge, "codeChallenge");
        Objects.requireNonNull(state, "state");
    }

    public static ZaloPkce generate() {
        return generate(new SecureRandom());
    }

    public static ZaloPkce generate(SecureRandom random) {
        Objects.requireNonNull(random, "random");
        char[] verifier = new char[43];
        verifier[0] = UPPER[random.nextInt(UPPER.length)];
        verifier[1] = LOWER[random.nextInt(LOWER.length)];
        verifier[2] = DIGITS[random.nextInt(DIGITS.length)];
        for (int i = 3; i < verifier.length; i++) {
            verifier[i] = ALL[random.nextInt(ALL.length)];
        }
        for (int i = verifier.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            char value = verifier[i];
            verifier[i] = verifier[swap];
            verifier[swap] = value;
        }
        String codeVerifier = new String(verifier);
        byte[] stateBytes = new byte[24];
        random.nextBytes(stateBytes);
        return new ZaloPkce(codeVerifier, challenge(codeVerifier),
                Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes));
    }

    public static String challenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
