package io.github.hoangluongtran0309.vietnam.einvoice.inspection;

import io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InvoiceInspectorTest {

    @Test
    void inspectsValidSyntheticInvoice() throws IOException {
        var result = InvoiceInspector.defaults().inspect(fixture("sales-no-buyer.xml"));

        assertThat(result.document().items()).hasSize(1);
        assertThat(result.signatures().checks()).isEmpty();
        assertThat(result.report().isValid()).isTrue();
    }

    @Test
    void disabledVerificationKeepsParsedMetadataAndOmitsSignatureIssues() throws IOException {
        var inspector = InvoiceInspector.builder().verifySignatures(false).build();

        var result = inspector.inspect(fixture("signed-metadata.xml"));

        assertThat(result.document().signatures()).hasSize(2);
        assertThat(result.signatures().checks()).isEmpty();
        assertThat(result.report().withCode("EINV-SIG-002")).isEmpty();
        assertThat(result.report().isValid()).isTrue();
    }

    @Test
    void propagatesParseErrors() {
        byte[] xml = "<NotAnInvoice/>".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(InvoiceXmlException.class)
                .isThrownBy(() -> InvoiceInspector.defaults().inspect(xml))
                .satisfies(error -> assertThat(error.code()).isEqualTo(InvoiceXmlErrorCode.UNEXPECTED_ROOT));
    }

    @Test
    void reportsFakeSignaturesAfterApplicationRulesWithoutChangingTheValidator() throws IOException {
        var applicationIssue = new ValidationIssue("APP-001", Severity.INFO, "HDon", "Application check.");
        var validator = InvoiceValidator.builder()
                .clock(Clock.fixed(Instant.parse("2026-11-26T00:00:00Z"), ZoneOffset.UTC))
                .addRule(document -> List.of(applicationIssue))
                .build();
        var inspector = InvoiceInspector.builder().validator(validator).build();

        var result = inspector.inspect(fixture("signed-metadata.xml"));

        assertThat(result.signatures().checks()).hasSize(2)
                .allSatisfy(check -> assertThat(check.status()).isEqualTo(SignatureStatus.MALFORMED));
        assertThat(result.report().isValid()).isFalse();
        assertThat(result.report().withCode("EINV-SIG-002")).extracting(ValidationIssue::path)
                .containsExactly("HDon/DSCKS/NBan/Signature", "HDon/DSCKS/CQT/Signature");
        assertThat(result.report().issues()).containsSubsequence(
                applicationIssue, result.report().withCode("EINV-SIG-002").get(0));
        assertThat(validator.validate(result.document()).withCode("EINV-SIG-002")).isEmpty();
        assertThat(inspector.inspect(fixture("sales-no-buyer.xml")).report().isValid()).isTrue();
    }

    private byte[] fixture(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/einvoice/synthetic/" + name)) {
            return input.readAllBytes();
        }
    }
}
