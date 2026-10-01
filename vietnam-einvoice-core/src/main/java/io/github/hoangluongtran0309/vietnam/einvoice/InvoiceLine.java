package io.github.hoangluongtran0309.vietnam.einvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InvoiceLine(
        String itemName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        TaxRate taxRate) {

    public BigDecimal amountBeforeTax() {
        return quantity.multiply(unitPrice).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal taxAmount() {
        // TODO(v0.3): confirm rounding rules per provider (line-level vs invoice-level rounding)
        return taxRate.rate() == null
                ? BigDecimal.ZERO
                : amountBeforeTax().multiply(taxRate.rate()).setScale(0, RoundingMode.HALF_UP);
    }
}
