package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.confirmation.PaymentConfirmation;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentReconcilerTest {

    private static final PaymentReferenceFormat FORMAT = PaymentReferenceFormat.of("DH");
    private static final String REFERENCE = FORMAT.encode("12345");
    private static final String OTHER_REFERENCE = FORMAT.encode("67890");
    private static final String ACCOUNT_NUMBER = "0123456789";
    private static final String BANK_BIN = "970436";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-16T02:30:00Z");

    @ParameterizedTest(name = "{0}")
    @MethodSource("reconciliationCases")
    void classifiesEachOutcomeInDecisionOrder(
            String description,
            PaymentConfirmation confirmation,
            Map<String, Payable> payables,
            ReconciliationResult expected) {
        PaymentReconciler reconciler = new PaymentReconciler(FORMAT,
                reference -> Optional.ofNullable(payables.get(reference)));

        assertThat(reconciler.reconcile(confirmation)).isEqualTo(expected);
    }

    static Stream<Arguments> reconciliationCases() {
        Payable open = payable(PayableStatus.OPEN, ACCOUNT_NUMBER, BANK_BIN);
        Payable settled = payable(PayableStatus.SETTLED, ACCOUNT_NUMBER, BANK_BIN);
        Payable cancelled = payable(PayableStatus.CANCELLED, ACCOUNT_NUMBER, BANK_BIN);
        Payable wrongAccountSettled = payable(PayableStatus.SETTLED, "9999999999", BANK_BIN);
        Payable wrongBinCancelled = payable(PayableStatus.CANCELLED, ACCOUNT_NUMBER, "970418");
        Payable withoutBin = payable(PayableStatus.OPEN, ACCOUNT_NUMBER, null);

        return Stream.of(
                arguments("no reference", confirmation("No payment code", 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(), new Unmatched(UnmatchedReason.NO_REFERENCE, List.of())),
                arguments("multiple references", confirmation(REFERENCE + " va " + OTHER_REFERENCE, 150_000,
                                ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, open),
                        new Unmatched(UnmatchedReason.MULTIPLE_REFERENCES, List.of(REFERENCE, OTHER_REFERENCE))),
                arguments("unknown reference", confirmation(OTHER_REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(), new Unmatched(UnmatchedReason.UNKNOWN_REFERENCE, List.of(OTHER_REFERENCE))),
                arguments("account mismatch wins over settled status",
                        confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, wrongAccountSettled), new WrongAccount(wrongAccountSettled)),
                arguments("BIN mismatch wins over cancelled status",
                        confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, wrongBinCancelled), new WrongAccount(wrongBinCancelled)),
                arguments("missing confirmation BIN does not reject a known account",
                        confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, null),
                        Map.of(REFERENCE, open), new Matched(open)),
                arguments("missing payable BIN does not reject a known account",
                        confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, withoutBin), new Matched(withoutBin)),
                arguments("settled status wins over an amount shortage",
                        confirmation(REFERENCE, 120_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, settled), new AlreadySettled(settled)),
                arguments("cancelled status wins over an amount excess",
                        confirmation(REFERENCE, 170_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, cancelled), new Cancelled(cancelled)),
                arguments("repeated same reference is still one candidate",
                        confirmation(REFERENCE + " " + REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, open), new Matched(open)),
                arguments("matched", confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, open), new Matched(open)),
                arguments("underpaid", confirmation(REFERENCE, 120_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, open), new Underpaid(open, 30_000)),
                arguments("overpaid", confirmation(REFERENCE, 170_000, ACCOUNT_NUMBER, BANK_BIN),
                        Map.of(REFERENCE, open), new Overpaid(open, 20_000)));
    }

    @Test
    void propagatesLookupFailure() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        PaymentReconciler reconciler = new PaymentReconciler(FORMAT, reference -> {
            throw failure;
        });

        assertThatThrownBy(() -> reconciler.reconcile(
                confirmation(REFERENCE, 150_000, ACCOUNT_NUMBER, BANK_BIN)))
                .isSameAs(failure);
    }

    @Test
    void unmatchedCandidatesAreAnImmutableCopy() {
        List<String> source = new ArrayList<>(List.of(REFERENCE));
        Unmatched unmatched = new Unmatched(UnmatchedReason.UNKNOWN_REFERENCE, source);
        source.set(0, OTHER_REFERENCE);

        assertThat(unmatched.candidates()).containsExactly(REFERENCE);
        assertThatThrownBy(() -> unmatched.candidates().add(OTHER_REFERENCE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Arguments arguments(
            String description,
            PaymentConfirmation confirmation,
            Map<String, Payable> payables,
            ReconciliationResult expected) {
        return Arguments.of(description, confirmation, payables, expected);
    }

    private static Payable payable(PayableStatus status, String accountNumber, String bankBin) {
        return new Payable(REFERENCE, 150_000, status, accountNumber, bankBin);
    }

    private static PaymentConfirmation confirmation(
            String content,
            long amount,
            String accountNumber,
            String bankBin) {
        return new PaymentConfirmation(
                "test", "TX-1", bankBin, accountNumber, amount, content, OCCURRED_AT, Map.of());
    }
}
