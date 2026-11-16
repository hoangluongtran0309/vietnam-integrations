package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.Objects;

/** The received amount exactly matches an open payable. */
public record Matched(Payable payable) implements ReconciliationResult {
    public Matched {
        Objects.requireNonNull(payable, "payable");
    }
}
