package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.Objects;

/** The payment reference belongs to a payable that is already settled. */
public record AlreadySettled(Payable payable) implements ReconciliationResult {
    public AlreadySettled {
        Objects.requireNonNull(payable, "payable");
    }
}
