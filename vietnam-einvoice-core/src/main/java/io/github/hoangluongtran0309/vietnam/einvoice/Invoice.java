package io.github.hoangluongtran0309.vietnam.einvoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Provider-agnostic invoice draft. Providers map this to their own request format.
 *
 * @param externalId idempotency key from the caller (e.g. order id); providers must not issue twice for it
 */
public record Invoice(
        String externalId,
        LocalDate issueDate,
        Party buyer,
        List<InvoiceLine> lines,
        String paymentMethod,
        String currency) {

    public Invoice {
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new EInvoiceException("Invoice must have at least one line");
        }
        if (currency == null) {
            currency = "VND";
        }
    }

    public BigDecimal totalBeforeTax() {
        return lines.stream().map(InvoiceLine::amountBeforeTax).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalTax() {
        return lines.stream().map(InvoiceLine::taxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPayable() {
        return totalBeforeTax().add(totalTax());
    }
}
