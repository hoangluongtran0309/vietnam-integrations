package io.github.hoangluongtran0309.vietnam.einvoice;

/**
 * SPI implemented by each provider adapter (Viettel, VNPT, MISA, BKAV, ...).
 */
public interface EInvoiceProvider {

    /** Stable id used in configuration, e.g. "viettel", "misa". */
    String id();

    IssuedInvoice issue(Invoice invoice);

    InvoiceStatus status(String externalId);

    byte[] downloadPdf(String externalId);

    // TODO(v0.3+): cancel, replace, adjust flows (hóa đơn thay thế / điều chỉnh)
}
