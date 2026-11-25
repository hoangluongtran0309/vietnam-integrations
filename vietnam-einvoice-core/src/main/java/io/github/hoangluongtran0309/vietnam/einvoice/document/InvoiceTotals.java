package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.math.BigDecimal;

/**
 * Totals read from {@code HDon/DLHDon/NDHDon/TToan} without recalculation.
 *
 * <p>Discount values retain their XML scale. TODO(v0.3): verify the meaning of TLCKhau, STCKhau and TTCKTMai — official QD 1233/QD-CT technical appendix.</p>
 *
 * @param totalAmountBeforeTax value at {@code HDon/DLHDon/NDHDon/TToan/TgTCThue}, with its XML scale preserved;
 *                             may be {@code null} when absent
 * @param totalTaxAmount value at {@code HDon/DLHDon/NDHDon/TToan/TgTThue}, with its XML scale preserved; may be
 *                       {@code null} when absent
 * @param totalTradeDiscount value at {@code HDon/DLHDon/NDHDon/TToan/TTCKTMai}; may be {@code null} when absent
 * @param totalPaymentAmount value at {@code HDon/DLHDon/NDHDon/TToan/TgTTTBSo}, with its XML scale preserved; may
 *                           be {@code null} when absent
 * @param totalPaymentAmountInWords value at {@code HDon/DLHDon/NDHDon/TToan/TgTTTBChu}; may be {@code null} when
 *                                  absent
 */
public record InvoiceTotals(
        BigDecimal totalAmountBeforeTax,
        BigDecimal totalTaxAmount,
        BigDecimal totalTradeDiscount,
        BigDecimal totalPaymentAmount,
        String totalPaymentAmountInWords) {
}
