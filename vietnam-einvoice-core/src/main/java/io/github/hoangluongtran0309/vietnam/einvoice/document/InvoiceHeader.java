package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * General information read from {@code HDon/DLHDon/TTChung}.
 *
 * @param formatVersion value at {@code HDon/DLHDon/TTChung/PBan}; may be {@code null} when absent;
 *                      TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 * @param invoiceName value at {@code HDon/DLHDon/TTChung/THDon}; may be {@code null} when absent
 * @param invoiceTemplateSymbol value at {@code HDon/DLHDon/TTChung/KHMSHDon}; may be {@code null} when absent;
 *                              TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 * @param invoiceSymbol value at {@code HDon/DLHDon/TTChung/KHHDon}; may be {@code null} when absent
 * @param invoiceNumber value at {@code HDon/DLHDon/TTChung/SHDon}; may be {@code null} when absent
 * @param issueDate value at {@code HDon/DLHDon/TTChung/NLap}; may be {@code null} when absent
 * @param financialLeasingFlag value at {@code HDon/DLHDon/TTChung/HDCTTChinh}; may be {@code null} when absent;
 *                             TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 * @param nonTariffAreaFlag value at {@code HDon/DLHDon/TTChung/HDDCKPTQuan}; may be {@code null} when absent;
 *                          TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 * @param currencyCode value at {@code HDon/DLHDon/TTChung/DVTTe}; may be {@code null} when absent
 * @param exchangeRate value at {@code HDon/DLHDon/TTChung/TGia}, with its XML scale preserved; may be {@code null}
 *                     when absent
 * @param paymentMethod value at {@code HDon/DLHDon/TTChung/HTTToan}; may be {@code null} when absent
 * @param solutionProviderTaxCode value at {@code HDon/DLHDon/TTChung/MSTTCGP}; may be {@code null} when absent
 */
public record InvoiceHeader(
        String formatVersion,
        String invoiceName,
        String invoiceTemplateSymbol,
        String invoiceSymbol,
        String invoiceNumber,
        LocalDate issueDate,
        Integer financialLeasingFlag,
        Integer nonTariffAreaFlag,
        String currencyCode,
        BigDecimal exchangeRate,
        String paymentMethod,
        String solutionProviderTaxCode) {
}
