package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Arrays;

/**
 * NAPAS 247 service codes carried in tag 38, sub-tag 02.
 */
public enum ServiceCode {

    /** Fast transfer to a bank account. */
    TO_ACCOUNT("QRIBFTTA"),

    /** Fast transfer to a card number. */
    TO_CARD("QRIBFTTC");

    private final String code;

    ServiceCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ServiceCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new VietQrException("Unknown service code: " + code));
    }
}
