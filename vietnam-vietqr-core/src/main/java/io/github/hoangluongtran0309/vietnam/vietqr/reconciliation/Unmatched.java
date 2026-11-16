package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.List;
import java.util.Objects;

/** A payment that could not be associated with exactly one known payable. */
public record Unmatched(UnmatchedReason reason, List<String> candidates) implements ReconciliationResult {
    public Unmatched {
        Objects.requireNonNull(reason, "reason");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
    }
}
