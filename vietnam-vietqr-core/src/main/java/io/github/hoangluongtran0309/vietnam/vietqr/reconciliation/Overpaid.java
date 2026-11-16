package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;

import java.util.Objects;

/** The received amount is higher than the amount expected for an open payable. */
public record Overpaid(Payable payable, long excessAmount) implements ReconciliationResult {
    public Overpaid {
        Objects.requireNonNull(payable, "payable");
        if (excessAmount <= 0) {
            throw new VietQrException("excessAmount must be positive: " + excessAmount);
        }
    }
}
