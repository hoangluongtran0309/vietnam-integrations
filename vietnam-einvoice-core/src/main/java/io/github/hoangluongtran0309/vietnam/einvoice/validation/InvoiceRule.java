package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;

import java.util.List;

/** An application rule applied to a parsed invoice. */
@FunctionalInterface
public interface InvoiceRule {

    /** Returns the findings in the rule's preferred order, or an empty list. */
    List<ValidationIssue> check(InvoiceDocument document);
}
