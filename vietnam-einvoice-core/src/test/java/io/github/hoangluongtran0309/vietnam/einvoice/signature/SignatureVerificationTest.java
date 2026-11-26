package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole.SELLER;
import static io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole.TAX_AUTHORITY;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.VALID;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.TrustStatus.NOT_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureVerificationTest {

    private static final String SELLER_PATH = "HDon/DSCKS/NBan/Signature[1]";
    private static final String AUTHORITY_PATH = "HDon/DSCKS/CQT/Signature";
    private static final InvoiceDocument UNCODED = parseHeader("K24TST");
    private static final InvoiceDocument CODED = parseHeader("C24TST");

    @Test
    void codedInvoiceWarnsOnlyWhenTheAuthoritySignatureIsAbsent() {
        assertIssue(new SignatureVerification(List.of()).asRule().check(CODED),
                "EINV-SIG-001", Severity.WARNING, "HDon/DSCKS/CQT");
        assertTrue(new SignatureVerification(List.of()).asRule().check(UNCODED).isEmpty());
        assertTrue(verification(TAX_AUTHORITY, VALID, NOT_CHECKED, true).asRule().check(CODED).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = SignatureStatus.class, names = {
            "INVALID_SIGNATURE", "INVALID_REFERENCE", "REFERENCE_NOT_COVERING_INVOICE",
            "MISSING_CERTIFICATE", "MALFORMED"
    })
    void verificationFailuresAreErrors(SignatureStatus status) {
        assertIssue(verification(SELLER, status, NOT_CHECKED, false).asRule().check(UNCODED),
                "EINV-SIG-002", Severity.ERROR, SELLER_PATH);
        assertTrue(verification(SELLER, VALID, NOT_CHECKED, false).asRule().check(UNCODED).isEmpty());
    }

    @Test
    void unsupportedAlgorithmWarns() {
        assertIssue(verification(SELLER, SignatureStatus.UNSUPPORTED_ALGORITHM, NOT_CHECKED, false)
                .asRule().check(UNCODED), "EINV-SIG-003", Severity.WARNING, SELLER_PATH);
        assertTrue(verification(SELLER, VALID, NOT_CHECKED, false).asRule().check(UNCODED).isEmpty());
    }

    @Test
    void certificateNotValidAtSigningTimeIsAnError() {
        assertIssue(verification(SELLER, SignatureStatus.CERTIFICATE_NOT_VALID_AT_SIGNING_TIME, NOT_CHECKED, false)
                .asRule().check(UNCODED), "EINV-SIG-004", Severity.ERROR, SELLER_PATH);
        assertTrue(verification(SELLER, VALID, NOT_CHECKED, false).asRule().check(UNCODED).isEmpty());
    }

    @Test
    void untrustedCertificateIsAnError() {
        assertIssue(verification(SELLER, VALID, TrustStatus.UNTRUSTED, false).asRule().check(UNCODED),
                "EINV-SIG-005", Severity.ERROR, SELLER_PATH);
        assertTrue(verification(SELLER, VALID, TrustStatus.TRUSTED, false).asRule().check(UNCODED).isEmpty());
        assertTrue(verification(SELLER, VALID, NOT_CHECKED, false).asRule().check(UNCODED).isEmpty());
    }

    @Test
    void validAuthoritySignatureWarnsWhenItOmitsTheTaxCode() {
        assertIssue(verification(TAX_AUTHORITY, VALID, NOT_CHECKED, false).asRule().check(CODED),
                "EINV-SIG-006", Severity.WARNING, AUTHORITY_PATH);
        assertTrue(verification(TAX_AUTHORITY, VALID, NOT_CHECKED, true).asRule().check(CODED).isEmpty());
        assertTrue(verification(TAX_AUTHORITY, SignatureStatus.UNSUPPORTED_ALGORITHM, NOT_CHECKED, false)
                .asRule().check(CODED).stream().noneMatch(issue -> issue.code().equals("EINV-SIG-006")));
    }

    @Test
    void parsesVerifiesAndValidatesTheSameBytesWithAnOptInRule() throws Exception {
        byte[] xml;
        try (var input = getClass().getResourceAsStream("/einvoice/synthetic/sales-no-buyer.xml")) {
            String invoice = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            xml = invoice.replace("K26MAU", "C26MAU")
                    .replace("</HDon>", "<MCCQT>synthetic-code</MCCQT></HDon>")
                    .getBytes(StandardCharsets.UTF_8);
        }
        var document = InvoiceXmlParser.defaults().parse(xml);
        var verification = InvoiceSignatureVerifier.defaults().verify(xml);
        var clock = Clock.fixed(Instant.parse("2026-11-27T00:00:00Z"), ZoneOffset.UTC);

        var defaultReport = InvoiceValidator.builder().clock(clock).build().validate(document);
        var signatureReport = InvoiceValidator.builder().clock(clock).addRule(verification.asRule())
                .build().validate(document);

        assertTrue(defaultReport.isValid());
        assertTrue(defaultReport.withCode("EINV-SIG-001").isEmpty());
        assertTrue(signatureReport.isValid());
        assertIssue(signatureReport.withCode("EINV-SIG-001"), "EINV-SIG-001", Severity.WARNING, "HDon/DSCKS/CQT");
        assertFalse(signatureReport.issues().stream().anyMatch(issue -> issue.code().equals("EINV-SIG-002")));
    }

    private static SignatureVerification verification(SignerRole role, SignatureStatus status,
                                                      TrustStatus trust, boolean taxCode) {
        String path = role == TAX_AUTHORITY ? AUTHORITY_PATH : SELLER_PATH;
        return new SignatureVerification(List.of(new SignatureCheck(role, path, status, trust,
                null, null, null, status == VALID, taxCode)));
    }

    private static void assertIssue(List<ValidationIssue> issues, String code, Severity severity, String path) {
        assertEquals(1, issues.size());
        assertEquals(code, issues.get(0).code());
        assertEquals(severity, issues.get(0).severity());
        assertEquals(path, issues.get(0).path());
    }

    private static InvoiceDocument parseHeader(String symbol) {
        String xml = "<HDon><DLHDon><TTChung><KHHDon>" + symbol + "</KHHDon></TTChung></DLHDon></HDon>";
        return InvoiceXmlParser.defaults().parse(xml.getBytes(StandardCharsets.UTF_8));
    }
}
