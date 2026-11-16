package io.github.hoangluongtran0309.vietnam.vietqr.reference;

import io.github.hoangluongtran0309.vietnam.vietqr.PurposeNormalizer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Encodes and extracts short payment references protected by a Luhn mod N check character.
 *
 * <p>A reference consists of an application-specific prefix, an application-provided body and one check character.
 * The alphabet is {@code 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ}. References are at most 25 characters long so they fit
 * the conservative VietQR purpose-length recommendation.
 */
public final class PaymentReferenceFormat {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RADIX = ALPHABET.length();
    private static final int MIN_PREFIX_LENGTH = 2;
    private static final int MAX_PREFIX_LENGTH = 6;
    private static final int MIN_BODY_LENGTH = 1;
    private static final int MAX_BODY_LENGTH = 20;
    private static final int MAX_REFERENCE_LENGTH = 25;

    private final String prefix;

    private PaymentReferenceFormat(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Creates a format for an uppercase ASCII prefix between 2 and 6 characters.
     *
     * @param prefix application-specific prefix
     * @return the payment reference format
     * @throws NullPointerException if {@code prefix} is {@code null}
     * @throws VietQrException if {@code prefix} does not contain 2-6 uppercase ASCII letters
     */
    public static PaymentReferenceFormat of(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (!prefix.matches("[A-Z]{" + MIN_PREFIX_LENGTH + "," + MAX_PREFIX_LENGTH + "}")) {
            throw new VietQrException("prefix must contain 2-6 uppercase ASCII letters: " + prefix);
        }
        return new PaymentReferenceFormat(prefix);
    }

    /**
     * Encodes an application-provided body and appends its Luhn mod N check character.
     *
     * @param body 1-20 ASCII letters or digits; letters are converted to uppercase
     * @return the canonical uppercase reference
     * @throws NullPointerException if {@code body} is {@code null}
     * @throws VietQrException if the body is invalid or the complete reference would exceed 25 characters
     */
    public String encode(String body) {
        Objects.requireNonNull(body, "body");
        if (!body.matches("[A-Za-z0-9]{" + MIN_BODY_LENGTH + "," + MAX_BODY_LENGTH + "}")) {
            throw new VietQrException("body must contain 1-20 ASCII letters or digits: " + body);
        }
        String normalizedBody = body.toUpperCase(Locale.ROOT);

        String payload = prefix + normalizedBody;
        if (payload.length() + 1 > MAX_REFERENCE_LENGTH) {
            throw new VietQrException("reference length must not exceed 25 characters: " + (payload.length() + 1));
        }
        return payload + checkCharacter(payload);
    }

    /**
     * Checks whether a reference is in canonical uppercase form, belongs to this prefix and has a valid check
     * character.
     *
     * @param reference reference to validate
     * @return {@code true} when the reference is valid, otherwise {@code false}
     */
    public boolean isValid(String reference) {
        if (reference == null || !reference.startsWith(prefix)) {
            return false;
        }
        int bodyLength = reference.length() - prefix.length() - 1;
        if (bodyLength < MIN_BODY_LENGTH
                || bodyLength > MAX_BODY_LENGTH
                || reference.length() > MAX_REFERENCE_LENGTH
                || !reference.matches("[A-Z0-9]+")) {
            return false;
        }

        String payload = reference.substring(0, reference.length() - 1);
        return reference.charAt(reference.length() - 1) == checkCharacter(payload);
    }

    /**
     * Extracts every valid reference from bank-recorded transfer content. Accents and case are ignored, punctuation
     * and whitespace delimit text, and references can be attached directly to surrounding letters or digits.
     * Duplicate references are removed while their first-seen order is retained.
     *
     * @param bankContent bank-recorded transfer content
     * @return an immutable list of valid references
     */
    public List<String> extract(String bankContent) {
        if (bankContent == null || bankContent.isBlank()) {
            return List.of();
        }

        String normalized = PurposeNormalizer.normalize(bankContent).toUpperCase(Locale.ROOT);
        String[] runs = normalized.split("[^A-Z0-9]+");
        Set<String> references = new LinkedHashSet<>();
        for (String run : runs) {
            extractFromRun(run, references);
        }
        return List.copyOf(references);
    }

    private void extractFromRun(String run, Set<String> references) {
        int prefixIndex = run.indexOf(prefix);
        while (prefixIndex >= 0) {
            int remaining = run.length() - prefixIndex;
            int maximumBodyLength = Math.min(
                    Math.min(MAX_BODY_LENGTH, MAX_REFERENCE_LENGTH - prefix.length() - 1),
                    remaining - prefix.length() - 1);
            for (int bodyLength = MIN_BODY_LENGTH; bodyLength <= maximumBodyLength; bodyLength++) {
                int end = prefixIndex + prefix.length() + bodyLength + 1;
                String candidate = run.substring(prefixIndex, end);
                if (isValid(candidate)) {
                    references.add(candidate);
                }
            }
            prefixIndex = run.indexOf(prefix, prefixIndex + 1);
        }
    }

    private static char checkCharacter(String payload) {
        int sum = 0;
        int factor = 2;
        for (int index = payload.length() - 1; index >= 0; index--) {
            int codePoint = ALPHABET.indexOf(payload.charAt(index));
            int addend = factor * codePoint;
            sum += addend / RADIX + addend % RADIX;
            factor = factor == 2 ? 1 : 2;
        }
        return ALPHABET.charAt((RADIX - sum % RADIX) % RADIX);
    }
}
