package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

/** Typed outcome of reconciling one payment confirmation against an application-owned payable. */
public sealed interface ReconciliationResult permits Matched, Underpaid, Overpaid, AlreadySettled, Cancelled,
        WrongAccount, Unmatched {
}
