package io.github.hoangluongtran0309.vietnam.einvoice;

/**
 * Result returned by a provider after issuing.
 *
 * @param templateCode  ký hiệu mẫu số
 * @param series        ký hiệu hóa đơn
 * @param invoiceNumber số hóa đơn
 * @param lookupCode    mã tra cứu for the buyer
 * @param taxAuthorityCode mã của cơ quan thuế, if applicable
 */
public record IssuedInvoice(
        String externalId,
        String providerId,
        String templateCode,
        String series,
        String invoiceNumber,
        String lookupCode,
        String taxAuthorityCode,
        InvoiceStatus status) {
}
