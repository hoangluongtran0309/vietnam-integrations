package io.github.hoangluongtran0309.vietnam.vietqr;

import java.nio.charset.StandardCharsets;

/**
 * CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF), as required by EMVCo MPM tag 63.
 */
public final class Crc16Ccitt {

    private Crc16Ccitt() {
    }

    public static int compute(String input) {
        int crc = 0xFFFF;
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

    public static String computeHex(String input) {
        return String.format("%04X", compute(input));
    }
}
