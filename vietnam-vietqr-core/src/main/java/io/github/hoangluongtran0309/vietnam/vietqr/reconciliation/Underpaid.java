package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;

import java.util.Objects;

/** The received amount is lower than the amount expected for an open payable. */
public record Underpaid(Payable payable, long missingAmount) implements ReconciliationResult {
    public Underpaid {
        Objects.requireNonNull(payable, "payable");
        if (missingAmount <= 0) {
            throw new VietQrException("missingAmount must be positive: " + missingAmount);
        }
    }
}
