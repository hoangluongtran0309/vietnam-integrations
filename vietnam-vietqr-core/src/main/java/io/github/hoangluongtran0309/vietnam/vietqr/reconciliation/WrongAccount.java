package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.Objects;

/** The payment was received into an account other than the one configured for the payable. */
public record WrongAccount(Payable payable) implements ReconciliationResult {
    public WrongAccount {
        Objects.requireNonNull(payable, "payable");
    }
}
