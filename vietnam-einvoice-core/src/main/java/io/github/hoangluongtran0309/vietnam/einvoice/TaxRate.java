package io.github.hoangluongtran0309.vietnam.einvoice;

import java.math.BigDecimal;

/**
 * VAT categories used on Vietnamese e-invoices.
 */
public enum TaxRate {
    RATE_0(new BigDecimal("0")),
    RATE_5(new BigDecimal("0.05")),
    RATE_8(new BigDecimal("0.08")),
    RATE_10(new BigDecimal("0.10")),
    /** Không chịu thuế. */
    NOT_SUBJECT(null),
    /** Không kê khai, tính nộp thuế. */
    NOT_DECLARED(null);

    private final BigDecimal rate;

    TaxRate(BigDecimal rate) {
        this.rate = rate;
    }

    /** Numeric rate, or {@code null} for non-percentage categories. */
    public BigDecimal rate() {
        return rate;
    }
}
