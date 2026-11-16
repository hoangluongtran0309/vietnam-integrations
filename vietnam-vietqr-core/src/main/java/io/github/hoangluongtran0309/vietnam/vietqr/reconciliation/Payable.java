package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;

import java.util.Objects;

/**
 * Application-owned amount expected for a payment reference.
 *
 * @param reference payment reference used in the VietQR purpose
 * @param expectedAmount expected amount in VND, positive
 * @param status current payable status
 * @param accountNumber receiving account number
 * @param bankBin 6-digit NAPAS BIN of the receiving account, or {@code null} when unavailable
 */
public record Payable(
        String reference,
        long expectedAmount,
        PayableStatus status,
        String accountNumber,
        String bankBin) {

    public Payable {
        requireText(reference, "reference");
        if (expectedAmount <= 0) {
            throw new VietQrException("expectedAmount must be positive: " + expectedAmount);
        }
        Objects.requireNonNull(status, "status");
        requireText(accountNumber, "accountNumber");
        if (bankBin != null && !bankBin.matches("\\d{6}")) {
            throw new VietQrException("bankBin must be exactly 6 digits: " + bankBin);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new VietQrException(name + " must not be blank");
        }
    }
}
