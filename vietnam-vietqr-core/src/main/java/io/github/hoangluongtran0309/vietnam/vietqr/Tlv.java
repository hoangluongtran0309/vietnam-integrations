package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal EMVCo TLV (ID: 2 digits, length: 2 digits, value) encoding and decoding.
 */
final class Tlv {

    private Tlv() {
    }

    static String field(String id, String value) {
        if (value.length() > 99) {
            throw new VietQrException("Value of tag " + id + " exceeds 99 characters");
        }
        return id + String.format("%02d", value.length()) + value;
    }

    static Map<String, String> parse(String data) {
        Map<String, String> fields = new LinkedHashMap<>();
        int i = 0;
        while (i < data.length()) {
            if (i + 4 > data.length()) {
                throw new VietQrException("Truncated TLV at position " + i);
            }
            String id = data.substring(i, i + 2);
            int length;
            try {
                length = Integer.parseInt(data.substring(i + 2, i + 4));
            } catch (NumberFormatException e) {
                throw new VietQrException("Invalid TLV length at position " + i, e);
            }
            int start = i + 4;
            int end = start + length;
            if (end > data.length()) {
                throw new VietQrException("TLV value of tag " + id + " overflows payload");
            }
            fields.put(id, data.substring(start, end));
            i = end;
        }
        return fields;
    }
}
