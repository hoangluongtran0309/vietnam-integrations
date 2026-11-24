package io.github.hoangluongtran0309.vietnam.einvoice.document;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceDocumentTest {

    @Test
    void normalizesNullCollectionsToEmptyCollections() {
        InvoiceDocument document = new InvoiceDocument(
                null, null, null, null, null, null, null, null, null);

        assertThat(document.items()).isEmpty();
        assertThat(document.taxRateSummaries()).isEmpty();
        assertThat(document.signatures()).isEmpty();
        assertThat(document.extensions()).isEmpty();
    }

    @Test
    void defensivelyCopiesCollectionsAndExposesImmutableViews() {
        InvoiceItem item = new InvoiceItem(null, null, null, null, null, null, null, null);
        TaxRateSummary summary = new TaxRateSummary(null, null, null);
        InvoiceSignature signature = new InvoiceSignature(SignerRole.SELLER, null, null);
        List<InvoiceItem> items = new ArrayList<>(List.of(item));
        List<TaxRateSummary> summaries = new ArrayList<>(List.of(summary));
        List<InvoiceSignature> signatures = new ArrayList<>(List.of(signature));
        Map<String, String> extensions = new HashMap<>(Map.of("HDon/Other", "value"));

        InvoiceDocument document = new InvoiceDocument(
                null, null, null, items, summaries, null, null, signatures, extensions);

        items.clear();
        summaries.clear();
        signatures.clear();
        extensions.clear();

        assertThat(document.items()).containsExactly(item);
        assertThat(document.taxRateSummaries()).containsExactly(summary);
        assertThat(document.signatures()).containsExactly(signature);
        assertThat(document.extensions()).containsEntry("HDon/Other", "value");
        assertThatThrownBy(() -> document.items().add(item)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> document.taxRateSummaries().add(summary))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> document.signatures().add(signature))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> document.extensions().put("HDon/New", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullListElementsWithParameterNames() {
        assertThatThrownBy(() -> new InvoiceDocument(
                null, null, null, Collections.singletonList(null), null, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items");
        assertThatThrownBy(() -> new InvoiceDocument(
                null, null, null, null, Collections.singletonList(null), null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("taxRateSummaries");
        assertThatThrownBy(() -> new InvoiceDocument(
                null, null, null, null, null, null, null, Collections.singletonList(null), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("signatures");
    }

    @Test
    void preservesExtensionsWithSameTagOnDifferentInvoiceItems() {
        String firstPath = "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[1]/STCKhau";
        String secondPath = "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/STCKhau";
        InvoiceDocument document = new InvoiceDocument(
                null, null, null, null, null, null, null, null,
                Map.of(firstPath, "100.00", secondPath, "200.00"));

        assertThat(document.extensions()).hasSize(2)
                .containsEntry(firstPath, "100.00")
                .containsEntry(secondPath, "200.00");
    }

    @Test
    void rejectsNullExtensionKeysAndValuesWithParameterName() {
        assertThatThrownBy(() -> new InvoiceDocument(
                null, null, null, null, null, null, null, null, Collections.singletonMap(null, "value")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("extensions");
        assertThatThrownBy(() -> new InvoiceDocument(
                null, null, null, null, null, null, null, null, Collections.singletonMap("HDon/Other", null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("extensions");
    }

    @Test
    void preservesBigDecimalScaleFromXmlValues() {
        InvoiceHeader header = new InvoiceHeader(
                "2.1.1", "Invoice", "1", "C26TAA", "00000001", LocalDate.of(2026, 11, 24),
                null, null, "USD", new BigDecimal("25000.0000"), "Transfer",
                "SOLUTION-PROVIDER-TAX-CODE-SENTINEL");
        InvoiceItem item = new InvoiceItem(
                1, "ITEM-1", "Service", "unit", new BigDecimal("1.000000"), new BigDecimal("1000.50"),
                new BigDecimal("1000.500000"), "10%");
        TaxRateSummary summary = new TaxRateSummary(
                "10%", new BigDecimal("1000.500000"), new BigDecimal("100.050000"));
        InvoiceTotals totals = new InvoiceTotals(
                new BigDecimal("1000.500000"), new BigDecimal("100.050000"), new BigDecimal("1100.550000"),
                "One thousand one hundred");

        InvoiceDocument document = new InvoiceDocument(
                header, null, null, List.of(item), List.of(summary), totals, null, null, null);

        assertThat(document.header().exchangeRate().scale()).isEqualTo(4);
        assertThat(document.items().get(0).quantity().scale()).isEqualTo(6);
        assertThat(document.items().get(0).unitPrice().scale()).isEqualTo(2);
        assertThat(document.items().get(0).amount().scale()).isEqualTo(6);
        assertThat(document.taxRateSummaries().get(0).amount().scale()).isEqualTo(6);
        assertThat(document.taxRateSummaries().get(0).taxAmount().scale()).isEqualTo(6);
        assertThat(document.totals().totalAmountBeforeTax().scale()).isEqualTo(6);
        assertThat(document.totals().totalTaxAmount().scale()).isEqualTo(6);
        assertThat(document.totals().totalPaymentAmount().scale()).isEqualTo(6);
    }

    @Test
    void acceptsMissingBuyerInvoiceNumberAndTaxAuthorityCode() {
        InvoiceHeader header = new InvoiceHeader(
                "2.1.1", "Invoice", "1", "C26TAA", null, LocalDate.of(2026, 11, 24),
                null, null, "VND", null, "Transfer", "SOLUTION-PROVIDER-TAX-CODE-SENTINEL");

        InvoiceDocument document = new InvoiceDocument(
                header, new InvoiceParty(null, null, null, null), null, null, null, null, null, null, null);

        assertThat(document.header().invoiceNumber()).isNull();
        assertThat(document.buyer()).isNull();
        assertThat(document.taxAuthorityCode()).isNull();
    }

    @Test
    void redactsPartyDataFromPartyAndDocumentStrings() {
        InvoiceParty seller = new InvoiceParty(
                "SELLER-NAME-SENTINEL", "SELLER-TAX-SENTINEL", "SELLER-ADDRESS-SENTINEL", null);
        InvoiceParty buyer = new InvoiceParty(
                "BUYER-NAME-SENTINEL", "BUYER-TAX-SENTINEL", "BUYER-ADDRESS-SENTINEL",
                "BUYER-FULL-NAME-SENTINEL");
        InvoiceDocument document = new InvoiceDocument(
                null, seller, buyer, null, null, null, null, null,
                Map.of("HDon/Other", "EXTENSION-VALUE-SENTINEL"));

        assertThat(seller.toString()).doesNotContain(
                "SELLER-NAME-SENTINEL", "SELLER-TAX-SENTINEL", "SELLER-ADDRESS-SENTINEL");
        assertThat(buyer.toString()).doesNotContain(
                "BUYER-NAME-SENTINEL", "BUYER-TAX-SENTINEL", "BUYER-ADDRESS-SENTINEL",
                "BUYER-FULL-NAME-SENTINEL");
        assertThat(document.toString()).doesNotContain(
                "SELLER-NAME-SENTINEL", "SELLER-TAX-SENTINEL", "SELLER-ADDRESS-SENTINEL",
                "BUYER-NAME-SENTINEL", "BUYER-TAX-SENTINEL", "BUYER-ADDRESS-SENTINEL",
                "BUYER-FULL-NAME-SENTINEL", "EXTENSION-VALUE-SENTINEL");
    }

    @Test
    void acceptsSignatureWithoutSigningTimeOrCertificate() {
        InvoiceSignature signature = new InvoiceSignature(SignerRole.SELLER, null, null);

        assertThat(signature.signingTime()).isNull();
        assertThat(signature.certificate()).isNull();
    }
}
