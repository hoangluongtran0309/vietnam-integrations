package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.confirmation.PaymentConfirmation;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Classifies payment confirmations using a reference format and application-owned payable lookup. */
public final class PaymentReconciler {

    private final PaymentReferenceFormat format;
    private final PayableLookup lookup;

    public PaymentReconciler(PaymentReferenceFormat format, PayableLookup lookup) {
        this.format = Objects.requireNonNull(format, "format");
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    /**
     * Reconciles one incoming payment without storing or changing application state.
     *
     * <p>The application must persist {@link PaymentConfirmation#idempotencyKey()} in the same transaction that
     * updates its payable. This class intentionally provides no duplicate-delivery protection. Exceptions from the
     * application-provided lookup are propagated so the confirmation adapter can report processing failure and allow
     * redelivery.
     *
     * @param confirmation incoming payment confirmation
     * @return a typed reconciliation outcome
     */
    public ReconciliationResult reconcile(PaymentConfirmation confirmation) {
        Objects.requireNonNull(confirmation, "confirmation");

        List<String> candidates = format.extract(confirmation.content());
        if (candidates.isEmpty()) {
            return new Unmatched(UnmatchedReason.NO_REFERENCE, candidates);
        }
        if (candidates.size() > 1) {
            return new Unmatched(UnmatchedReason.MULTIPLE_REFERENCES, candidates);
        }

        Optional<Payable> found = Objects.requireNonNull(
                lookup.findByReference(candidates.get(0)),
                "PayableLookup.findByReference returned null");
        if (found.isEmpty()) {
            return new Unmatched(UnmatchedReason.UNKNOWN_REFERENCE, candidates);
        }

        Payable payable = found.get();
        if (isWrongAccount(confirmation, payable)) {
            return new WrongAccount(payable);
        }
        if (payable.status() == PayableStatus.SETTLED) {
            return new AlreadySettled(payable);
        }
        if (payable.status() == PayableStatus.CANCELLED) {
            return new Cancelled(payable);
        }

        long difference = confirmation.amount() - payable.expectedAmount();
        if (difference == 0) {
            return new Matched(payable);
        }
        if (difference < 0) {
            return new Underpaid(payable, -difference);
        }
        return new Overpaid(payable, difference);
    }

    private static boolean isWrongAccount(PaymentConfirmation confirmation, Payable payable) {
        if (!payable.accountNumber().equals(confirmation.accountNumber())) {
            return true;
        }
        return payable.bankBin() != null
                && confirmation.bankBin() != null
                && !payable.bankBin().equals(confirmation.bankBin());
    }
}
