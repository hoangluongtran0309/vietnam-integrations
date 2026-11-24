package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.math.BigDecimal;

/**
 * Totals read from {@code HDon/DLHDon/NDHDon/TToan} without recalculation.
 *
 * @param totalAmountBeforeTax value at {@code HDon/DLHDon/NDHDon/TToan/TgTCThue}, with its XML scale preserved;
 *                             may be {@code null} when absent
 * @param totalTaxAmount value at {@code HDon/DLHDon/NDHDon/TToan/TgTThue}, with its XML scale preserved; may be
 *                       {@code null} when absent
 * @param totalPaymentAmount value at {@code HDon/DLHDon/NDHDon/TToan/TgTTTBSo}, with its XML scale preserved; may
 *                           be {@code null} when absent
 * @param totalPaymentAmountInWords value at {@code HDon/DLHDon/NDHDon/TToan/TgTTTBChu}; may be {@code null} when
 *                                  absent
 */
public record InvoiceTotals(
        BigDecimal totalAmountBeforeTax,
        BigDecimal totalTaxAmount,
        BigDecimal totalPaymentAmount,
        String totalPaymentAmountInWords) {
}
