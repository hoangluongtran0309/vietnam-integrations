package io.github.hoangluongtran0309.vietnam.vietqr;

import java.text.Normalizer;

/**
 * Many banking apps render non-ASCII transfer content incorrectly, so purposes are reduced to printable ASCII.
 */
public final class PurposeNormalizer {

    private PurposeNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String ascii = decomposed.replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "");
        return ascii.trim().replaceAll("\\s+", " ");
    }
}
