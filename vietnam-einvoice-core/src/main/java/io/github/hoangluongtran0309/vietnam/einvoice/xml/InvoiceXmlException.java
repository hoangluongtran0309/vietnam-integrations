package io.github.hoangluongtran0309.vietnam.einvoice.xml;

import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceException;

/** Reports an invoice XML input error without exposing element values or the underlying XML reader diagnostic. */
public final class InvoiceXmlException extends EInvoiceException {

    private static final long serialVersionUID = 1L;

    private final InvoiceXmlErrorCode code;
    private final String path;

    /**
     * Creates an input error from a stable code and a path containing only element names and indices.
     *
     * @param code the XML input error code
     * @param path the element path, or {@code null} when unavailable
     */
    public InvoiceXmlException(InvoiceXmlErrorCode code, String path) {
        super(code + (path == null ? "" : " at " + path) + ": " + code.description());
        this.code = code;
        this.path = path;
    }

    /** @return the stable error code for this failure */
    public InvoiceXmlErrorCode code() {
        return code;
    }

    /** @return the XML element path, or {@code null} if no element path is available */
    public String path() {
        return path;
    }
}
