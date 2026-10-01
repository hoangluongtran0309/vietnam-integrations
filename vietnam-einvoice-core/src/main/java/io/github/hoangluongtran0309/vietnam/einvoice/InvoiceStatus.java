package io.github.hoangluongtran0309.vietnam.einvoice;

public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    SENT_TO_TAX_AUTHORITY,
    ACCEPTED_BY_TAX_AUTHORITY,
    REJECTED_BY_TAX_AUTHORITY,
    CANCELLED,
    REPLACED,
    ADJUSTED
}
