package io.github.hoangluongtran0309.vietnam.einvoice;

public class EInvoiceException extends RuntimeException {

    public EInvoiceException(String message) {
        super(message);
    }

    public EInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
