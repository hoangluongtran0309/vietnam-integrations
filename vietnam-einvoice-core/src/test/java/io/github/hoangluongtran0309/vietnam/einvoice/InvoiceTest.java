package io.github.hoangluongtran0309.vietnam.einvoice;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceTest {

    @Test
    void computesTotalsAcrossTaxRates() {
        Invoice invoice = new Invoice("ORDER-1", LocalDate.of(2026, 10, 1),
                new Party("Nguyen Van A", null, "TP.HCM", null, null),
                List.of(
                        new InvoiceLine("Sach", "cuon", new BigDecimal("2"), new BigDecimal("100000"), TaxRate.RATE_5),
                        new InvoiceLine("Dich vu", "lan", BigDecimal.ONE, new BigDecimal("50000"), TaxRate.RATE_10)),
                "TM/CK", null);

        assertThat(invoice.totalBeforeTax()).isEqualByComparingTo("250000");
        assertThat(invoice.totalTax()).isEqualByComparingTo("15000");
        assertThat(invoice.totalPayable()).isEqualByComparingTo("265000");
        assertThat(invoice.currency()).isEqualTo("VND");
    }
}
