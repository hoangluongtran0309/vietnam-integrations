package io.github.hoangluongtran0309.vietnam.einvoice.inspection;

import io.github.hoangluongtran0309.vietnam.einvoice.signature.InvoiceSignatureVerifier;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureVerification;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;

import java.util.List;
import java.util.Objects;

/** Reads, verifies signatures on, and validates issued invoice XML. */
public final class InvoiceInspector {

    private static final SignatureVerification EMPTY_VERIFICATION = new SignatureVerification(List.of());

    private final InvoiceXmlParser parser;
    private final InvoiceSignatureVerifier signatureVerifier;
    private final InvoiceValidator validator;
    private final boolean verifySignatures;

    private InvoiceInspector(Builder builder) {
        parser = Objects.requireNonNull(builder.parser, "parser");
        signatureVerifier = Objects.requireNonNull(builder.signatureVerifier, "signatureVerifier");
        validator = Objects.requireNonNull(builder.validator, "validator");
        verifySignatures = builder.verifySignatures;
    }

    /** Returns an inspector with default components and signature verification enabled. */
    public static InvoiceInspector defaults() {
        return builder().build();
    }

    /** Returns a builder initialized with default components and signature verification enabled. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses the original XML bytes, verifies signatures when enabled, and validates the document.
     * Signature issues follow the existing validation rules.
     *
     * @param xml the original invoice XML bytes
     * @return the document, signature checks, and validation report
     * @throws InvoiceXmlException if the parser or signature verifier rejects the XML input
     */
    public InvoiceInspection inspect(byte[] xml) {
        var document = parser.parse(xml);
        if (!verifySignatures) {
            return new InvoiceInspection(document, EMPTY_VERIFICATION, validator.validate(document));
        }
        var signatures = signatureVerifier.verify(xml);
        return new InvoiceInspection(document, signatures, validator.withRule(signatures.asRule()).validate(document));
    }

    /** Configures the components used to inspect an invoice. */
    public static final class Builder {

        private InvoiceXmlParser parser = InvoiceXmlParser.defaults();
        private InvoiceSignatureVerifier signatureVerifier = InvoiceSignatureVerifier.defaults();
        private InvoiceValidator validator = InvoiceValidator.defaults();
        private boolean verifySignatures = true;

        private Builder() {
        }

        /** Sets the XML parser. */
        public Builder parser(InvoiceXmlParser parser) {
            this.parser = parser;
            return this;
        }

        /** Sets the signature verifier. */
        public Builder signatureVerifier(InvoiceSignatureVerifier signatureVerifier) {
            this.signatureVerifier = signatureVerifier;
            return this;
        }

        /** Sets the invoice validator. */
        public Builder validator(InvoiceValidator validator) {
            this.validator = validator;
            return this;
        }

        /** Controls whether the inspector verifies signatures and adds their issues to the report. */
        public Builder verifySignatures(boolean verifySignatures) {
            this.verifySignatures = verifySignatures;
            return this;
        }

        /** Builds an inspector and rejects null components. */
        public InvoiceInspector build() {
            return new InvoiceInspector(this);
        }
    }
}
