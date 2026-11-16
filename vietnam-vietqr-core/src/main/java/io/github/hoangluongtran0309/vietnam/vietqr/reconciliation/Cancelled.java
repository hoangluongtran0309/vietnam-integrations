package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.Objects;

/** The payment reference belongs to a cancelled payable. */
public record Cancelled(Payable payable) implements ReconciliationResult {
    public Cancelled {
        Objects.requireNonNull(payable, "payable");
    }
}
