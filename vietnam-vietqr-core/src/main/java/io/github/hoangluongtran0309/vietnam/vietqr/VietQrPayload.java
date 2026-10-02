package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Optional;

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

    /** The beneficiary bank, or empty if {@link #bankBin()} is not in {@link VietQrBank}. */
    public Optional<VietQrBank> bank() {
        return VietQrBank.fromBin(bankBin);
    }
}
