package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

/** Reason why a payment confirmation could not be associated with one payable. */
public enum UnmatchedReason {
    NO_REFERENCE,
    MULTIPLE_REFERENCES,
    UNKNOWN_REFERENCE
}
