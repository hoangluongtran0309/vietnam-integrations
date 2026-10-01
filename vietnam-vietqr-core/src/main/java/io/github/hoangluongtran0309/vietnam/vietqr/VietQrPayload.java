package io.github.hoangluongtran0309.vietnam.vietqr;

/**
 * Decoded view of a VietQR payload.
 */
public record VietQrPayload(
        String bankBin,
        String accountNumber,
        ServiceCode serviceCode,
        Long amount,
        String purpose,
        boolean dynamic) {
}
