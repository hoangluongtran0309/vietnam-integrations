package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceRule;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * The checks of the invoice signatures in document order.
 *
 * @param checks signature checks, defensively copied
 */
public record SignatureVerification(List<SignatureCheck> checks) {

    public SignatureVerification {
        checks = List.copyOf(checks);
    }

    /**
     * Returns an opt-in rule for the invoice parsed from the same XML bytes as these checks.
     * Add it to the validator for that invoice with {@code InvoiceValidator.Builder.addRule}.
     */
    public InvoiceRule asRule() {
        return this::issues;
    }

    private List<ValidationIssue> issues(InvoiceDocument document) {
        var issues = new ArrayList<ValidationIssue>();
        String symbol = document.header() == null ? null : document.header().invoiceSymbol();
        if (symbol != null && symbol.startsWith("C")
                && checks.stream().noneMatch(check -> check.role() == SignerRole.TAX_AUTHORITY)) {
            issues.add(new ValidationIssue("EINV-SIG-001", Severity.WARNING, "HDon/DSCKS/CQT",
                    "A coded invoice has no tax authority signature."));
        }

        for (SignatureCheck check : checks) {
            var issue = statusIssue(check);
            if (issue != null) {
                issues.add(issue);
            }
            if (check.trust() == TrustStatus.UNTRUSTED) {
                issues.add(new ValidationIssue("EINV-SIG-005", Severity.ERROR, check.path(),
                        "The signing certificate could not be trusted at the signing time."));
            }
            if (check.role() == SignerRole.TAX_AUTHORITY && check.status() == SignatureStatus.VALID
                    && !check.coversTaxAuthorityCode()) {
                issues.add(new ValidationIssue("EINV-SIG-006", Severity.WARNING, check.path(),
                        "The tax authority signature does not cover the tax authority code."));
            }
        }
        return List.copyOf(issues);
    }

    private static ValidationIssue statusIssue(SignatureCheck check) {
        String path = check.path();
        return switch (check.status()) {
            case INVALID_SIGNATURE -> verificationError(path, "The signature value could not be verified.");
            case INVALID_REFERENCE -> verificationError(path, "A signature reference could not be validated.");
            case REFERENCE_NOT_COVERING_INVOICE -> verificationError(path, "The signature does not cover the invoice data.");
            case MISSING_CERTIFICATE -> verificationError(path, "The signature has no X.509 signing certificate.");
            case MALFORMED -> verificationError(path, "The XML signature structure is malformed.");
            case UNSUPPORTED_ALGORITHM -> new ValidationIssue("EINV-SIG-003", Severity.WARNING, path,
                    "The signature uses an unsupported algorithm.");
            case CERTIFICATE_NOT_VALID_AT_SIGNING_TIME -> new ValidationIssue("EINV-SIG-004", Severity.ERROR, path,
                    "The signing certificate was outside its validity period at the signing time.");
            case VALID -> null;
        };
    }

    private static ValidationIssue verificationError(String path, String message) {
        return new ValidationIssue("EINV-SIG-002", Severity.ERROR, path, message);
    }
}
