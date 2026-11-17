package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure.VietQrGenerator;
import io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure.VietQrProperties;
import io.github.hoangluongtran0309.vietnam.vietqr.confirmation.PaymentConfirmation;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.AlreadySettled;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Cancelled;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Matched;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Overpaid;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Payable;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableStatus;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PaymentReconciler;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.ReconciliationResult;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Underpaid;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Unmatched;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.WrongAccount;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Offline endpoints for trying payment-reference reconciliation without a provider adapter. */
@RestController
class ReconciliationDemoController {

    private final VietQrGenerator vietQr;
    private final VietQrProperties properties;
    private final PaymentReferenceFormat referenceFormat;
    private final PaymentReconciler reconciler;
    private final InMemoryPayableLookup payables;
    private final AtomicLong orderSequence = new AtomicLong(100_000);
    private final AtomicLong transactionSequence = new AtomicLong();

    ReconciliationDemoController(
            VietQrGenerator vietQr,
            VietQrProperties properties,
            PaymentReferenceFormat referenceFormat,
            PaymentReconciler reconciler,
            InMemoryPayableLookup payables) {
        this.vietQr = vietQr;
        this.properties = properties;
        this.referenceFormat = referenceFormat;
        this.reconciler = reconciler;
        this.payables = payables;
    }

    @GetMapping("/checkout")
    CheckoutResponse checkout(@RequestParam(defaultValue = "150000") long amount) {
        requirePositiveAmount(amount);

        String orderId = Long.toString(orderSequence.incrementAndGet());
        String reference = referenceFormat.encode(orderId);
        payables.save(new Payable(
                reference,
                amount,
                PayableStatus.OPEN,
                properties.getAccountNumber(),
                properties.getBankBin()));

        return new CheckoutResponse(orderId, reference, amount, vietQr.forReference(amount, reference));
    }

    @PostMapping("/demo/payments/simulate")
    SimulationResponse simulate(@RequestBody SimulationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        requirePositiveAmount(request.amount());

        PaymentConfirmation confirmation = new PaymentConfirmation(
                "demo",
                "simulated-" + transactionSequence.incrementAndGet(),
                properties.getBankBin(),
                properties.getAccountNumber(),
                request.amount(),
                request.content(),
                Instant.now(),
                Map.of());
        return SimulationResponse.from(reconciler.reconcile(confirmation));
    }

    private static void requirePositiveAmount(long amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
    }

    record CheckoutResponse(String orderId, String reference, long amount, String qrPayload) {
    }

    record SimulationRequest(String content, long amount) {
    }

    record SimulationResponse(
            String outcome,
            String reference,
            String reason,
            Long missingAmount,
            Long excessAmount,
            List<String> candidates) {

        static SimulationResponse from(ReconciliationResult result) {
            if (result instanceof Matched matched) {
                return payableOutcome("MATCHED", matched.payable());
            }
            if (result instanceof Underpaid underpaid) {
                return new SimulationResponse(
                        "UNDERPAID", underpaid.payable().reference(), null,
                        underpaid.missingAmount(), null, List.of());
            }
            if (result instanceof Overpaid overpaid) {
                return new SimulationResponse(
                        "OVERPAID", overpaid.payable().reference(), null,
                        null, overpaid.excessAmount(), List.of());
            }
            if (result instanceof AlreadySettled settled) {
                return payableOutcome("ALREADY_SETTLED", settled.payable());
            }
            if (result instanceof Cancelled cancelled) {
                return payableOutcome("CANCELLED", cancelled.payable());
            }
            if (result instanceof WrongAccount wrongAccount) {
                return payableOutcome("WRONG_ACCOUNT", wrongAccount.payable());
            }
            if (result instanceof Unmatched unmatched) {
                return new SimulationResponse(
                        "UNMATCHED", null, unmatched.reason().name(),
                        null, null, unmatched.candidates());
            }
            throw new IllegalStateException("Unsupported reconciliation result: " + result.getClass().getName());
        }

        private static SimulationResponse payableOutcome(String outcome, Payable payable) {
            return new SimulationResponse(outcome, payable.reference(), null, null, null, List.of());
        }
    }
}
