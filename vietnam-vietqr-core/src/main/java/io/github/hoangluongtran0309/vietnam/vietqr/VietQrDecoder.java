package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Map;

/**
 * Parses and validates a VietQR payload (including CRC).
 */
public final class VietQrDecoder {

    public VietQrPayload decode(String payload) {
        if (payload == null || payload.length() < 8) {
            throw new VietQrException("Payload too short");
        }
        verifyCrc(payload);

        Map<String, String> root = Tlv.parse(payload);
        String merchantAccount = require(root, "38");
        Map<String, String> account = Tlv.parse(merchantAccount);
        if (!VietQrEncoder.NAPAS_GUID.equals(account.get("00"))) {
            throw new VietQrException("Not a NAPAS VietQR payload (unexpected GUID)");
        }
        Map<String, String> beneficiary = Tlv.parse(require(account, "01"));

        String amount = root.get("54");
        String purpose = root.containsKey("62") ? Tlv.parse(root.get("62")).get("08") : null;

        return new VietQrPayload(
                require(beneficiary, "00"),
                require(beneficiary, "01"),
                ServiceCode.fromCode(require(account, "02")),
                amount == null ? null : parseAmount(amount),
                purpose,
                "12".equals(root.get("01")));
    }

    private static void verifyCrc(String payload) {
        int crcStart = payload.length() - 4;
        if (!payload.startsWith("6304", crcStart - 4)) {
            throw new VietQrException("Payload must end with CRC tag 63");
        }
        String expected = Crc16Ccitt.computeHex(payload.substring(0, crcStart));
        String actual = payload.substring(crcStart).toUpperCase();
        if (!expected.equals(actual)) {
            throw new VietQrException("CRC mismatch: expected " + expected + " but was " + actual);
        }
    }

    private static Long parseAmount(String amount) {
        try {
            return Long.parseLong(amount);
        } catch (NumberFormatException e) {
            // TODO: decide whether to support decimal amounts (EMVCo allows them, VND normally does not)
            throw new VietQrException("Unsupported amount format: " + amount, e);
        }
    }

    private static String require(Map<String, String> fields, String tag) {
        String value = fields.get(tag);
        if (value == null) {
            throw new VietQrException("Missing required tag " + tag);
        }
        return value;
    }
}
