package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspector;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureCheck;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.TrustStatus;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Inspects an uploaded invoice XML in memory and returns a summary of the findings. */
@RestController
class EInvoiceInspectionController {

    private final InvoiceInspector inspector;

    EInvoiceInspectionController(InvoiceInspector inspector) {
        this.inspector = inspector;
    }

    /** {@code curl -F file=@invoice.xml localhost:8080/einvoice/inspect} */
    @PostMapping(value = "/einvoice/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    InspectionResult inspect(@RequestParam("file") MultipartFile file) throws IOException {
        var inspection = inspector.inspect(file.getBytes());
        var document = inspection.document();
        var summary = new DocumentSummary(
                HeaderSummary.from(document.header()),
                document.items().size(),
                document.totals() == null ? null : document.totals().totalPaymentAmount(),
                document.taxAuthorityCode() != null && !document.taxAuthorityCode().isBlank());
        return new InspectionResult(summary, inspection.report().isValid(), inspection.report().issues(),
                inspection.signatures().checks().stream().map(SignatureSummary::from).toList());
    }

    @ExceptionHandler(InvoiceXmlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    XmlError invalidXml(InvoiceXmlException error) {
        return new XmlError(error.code(), error.path());
    }

    record InspectionResult(DocumentSummary document, boolean isValid, List<ValidationIssue> issues,
                            List<SignatureSummary> signatures) {

        @Override
        public String toString() {
            // Spring MVC can log the response object's string representation at DEBUG level.
            return "InspectionResult";
        }
    }

    record DocumentSummary(HeaderSummary header, int lineCount, BigDecimal totalPaymentAmount,
                           boolean hasTaxAuthorityCode) {
    }

    record HeaderSummary(String formatVersion, String invoiceTemplateSymbol, String invoiceSymbol, String invoiceNumber,
                         LocalDate issueDate, String currencyCode) {

        static HeaderSummary from(InvoiceHeader header) {
            return header == null ? null : new HeaderSummary(header.formatVersion(), header.invoiceTemplateSymbol(),
                    header.invoiceSymbol(), header.invoiceNumber(), header.issueDate(), header.currencyCode());
        }
    }

    record SignatureSummary(SignerRole role, String path, SignatureStatus status, TrustStatus trust,
                            String algorithm, Instant signingTime) {

        static SignatureSummary from(SignatureCheck check) {
            return new SignatureSummary(check.role(), check.path(), check.status(), check.trust(),
                    check.signatureAlgorithm(), check.signingTime());
        }
    }

    record XmlError(InvoiceXmlErrorCode code, String path) {
    }
}
