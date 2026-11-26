package io.github.hoangluongtran0309.vietnam.einvoice.inspection;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureVerification;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationReport;

import java.util.Objects;

/**
 * The document and findings from a single invoice inspection.
 *
 * @param document the parsed invoice
 * @param signatures the signature verification results, with no checks when verification is disabled
 * @param report the validation report, including signature issues when verification is enabled
 */
public record InvoiceInspection(InvoiceDocument document, SignatureVerification signatures, ValidationReport report) {

    public InvoiceInspection {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(signatures, "signatures");
        Objects.requireNonNull(report, "report");
    }
}
