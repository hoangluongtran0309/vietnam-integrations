package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.math.BigDecimal;

/**
 * One tax-rate aggregate read from {@code HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat}.
 *
 * @param taxRate value at {@code HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TSuat}, preserved as written; may be
 *                {@code null} when absent; TODO(v0.3): verify valid values — official Decision 1233/QD-CT and
 *                official XSD schema
 * @param amount value at {@code HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/ThTien}, with its XML scale preserved;
 *               may be {@code null} when absent
 * @param taxAmount value at {@code HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TThue}, with its XML scale preserved;
 *                  may be {@code null} when absent
 */
public record TaxRateSummary(String taxRate, BigDecimal amount, BigDecimal taxAmount) {
}
