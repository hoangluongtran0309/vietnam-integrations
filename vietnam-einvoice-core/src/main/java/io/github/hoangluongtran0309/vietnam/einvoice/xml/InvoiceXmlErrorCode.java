package io.github.hoangluongtran0309.vietnam.einvoice.xml;

/** Stable error codes for applications that parse invoice XML. */
public enum InvoiceXmlErrorCode {
    /** The byte array or stream exceeds the configured byte limit. */
    INPUT_TOO_LARGE("XML input exceeds the byte limit"),
    /** The input could not be decoded as well-formed XML. */
    MALFORMED_XML("Input is not well-formed XML"),
    /** The XML contains a DOCTYPE declaration, including an internal-only declaration. */
    DOCTYPE_NOT_ALLOWED("DOCTYPE declarations are forbidden"),
    /** The root element does not have the local name HDon. */
    UNEXPECTED_ROOT("Expected HDon as the root element"),
    /** A single-valued element appears more than once under the same parent. */
    DUPLICATE_ELEMENT("Single-valued element appears more than once"),
    /** An element could not be read as a decimal number. */
    INVALID_NUMBER("Expected a decimal number"),
    /** An element could not be read as a signed 32-bit integer. */
    INVALID_INTEGER("Expected a signed 32-bit integer"),
    /** An element could not be read as the required date or offset date-time. */
    INVALID_DATE("Expected a valid date or offset date-time"),
    /** A certificate element could not be read as a Base64-encoded X.509 certificate. */
    INVALID_CERTIFICATE("Expected a Base64-encoded X.509 certificate"),
    /** Reading the supplied stream failed. */
    IO_ERROR("Could not read the XML input"),
    /** The document exceeds the configured element count, including its root element. */
    TOO_MANY_ELEMENTS("XML input exceeds the element limit");

    private final String description;

    InvoiceXmlErrorCode(String description) {
        this.description = description;
    }

    String description() {
        return description;
    }
}
