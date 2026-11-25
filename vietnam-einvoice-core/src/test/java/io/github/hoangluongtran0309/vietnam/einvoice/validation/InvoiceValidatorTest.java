package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceItem;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceSignature;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceTotals;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.document.TaxRateSummary;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.ERROR;
import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.INFO;
import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class InvoiceValidatorTest {

    private static final String HEADER = "HDon/DLHDon/TTChung/";
    private static final String CONTENT = "HDon/DLHDon/NDHDon/";
    private static final String ITEMS = CONTENT + "DSHHDVu";
    private static final String TOTALS = CONTENT + "TToan";
    private static final String GROUPS = TOTALS + "/THTTLTSuat/LTSuat";
    private static final String SIGNING_TIME =
            "/Object/QualifyingProperties/SignedProperties/SignedSignatureProperties/SigningTime";
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant START_OF_DAY = Instant.parse("2026-11-24T17:00:00Z");
    private static final Clock CLOCK = Clock.fixed(START_OF_DAY, VIETNAM);
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    private static final InvoiceHeader HEADER_VAT = header("1", "K26TMA", TODAY, 0, null, "VND", null, "0000000000");
    private static final InvoiceParty SELLER = new InvoiceParty("Sample seller", "0000000000-000", "Sample address", null);
    private static final List<InvoiceItem> LINES = List.of(line(1, "100", "10%"));
    private static final List<TaxRateSummary> GROUP = List.of(group("10%", "100", "10"));
    private static final InvoiceTotals VAT_TOTALS = totals("100", "10", "110", null);
    private static final InvoiceTotals SALES_TOTALS = totals(null, null, "110", null);

    private final InvoiceValidator validator = InvoiceValidator.builder().clock(CLOCK).build();

    @ParameterizedTest
    @ValueSource(strings = {"vat-two-rates.xml", "vat-with-extensions.xml", "vat-line-discount.xml", "sales-no-buyer.xml",
            "signed-metadata.xml", "utf16-encoded.xml"})
    void acceptsSyntheticFixtures(String fixture) throws IOException {
        try (var input = getClass().getResourceAsStream("/einvoice/synthetic/" + fixture)) {
            assertThat(validator.validate(InvoiceXmlParser.defaults().parse(input)).errors()).isEmpty();
        }
    }

    @Test
    void acceptsCompleteInvoiceWithExactAmounts() {
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, GROUP, VAT_TOTALS)).issues()).isEmpty();
    }

    @Test
    void reportsAllMissingFieldsInOrderWhenDocumentIsEmpty() {
        var report = validator.validate(new InvoiceDocument(null, null, null, null, null, null, null, null, null));

        assertThat(report.withCode("EINV-REQ-001")).extracting(ValidationIssue::path).containsExactly(
                HEADER + "PBan", HEADER + "THDon", HEADER + "KHMSHDon", HEADER + "KHHDon", HEADER + "NLap",
                HEADER + "DVTTe", HEADER + "HTTToan", HEADER + "MSTTCGP",
                CONTENT + "NBan/Ten", CONTENT + "NBan/MST", CONTENT + "NBan/DChi",
                TOTALS + "/TgTTTBSo", TOTALS + "/TgTTTBChu");
        assertFinding(report, "EINV-REQ-002", ERROR, ITEMS);
        assertThat(report.issues()).hasSize(14);
        assertThat(report.isValid()).isFalse();
    }

    @Test
    void reportsMissingAndBlankFieldsOnIndexedLines() {
        var missing = new InvoiceItem(null, null, " ", null, null, null, null, null, null, null);
        var report = validator.validate(invoice(HEADER_VAT, List.of(missing, missing), GROUP, VAT_TOTALS));

        assertThat(report.withCode("EINV-REQ-001")).extracting(ValidationIssue::path).containsExactly(
                ITEMS + "/HHDVu[1]/TChat", ITEMS + "/HHDVu[1]/THHDVu",
                ITEMS + "/HHDVu[2]/TChat", ITEMS + "/HHDVu[2]/THHDVu");
        assertThat(report.withCode("EINV-REQ-002")).isEmpty();
    }

    @Test
    void requiresVatFieldsOnlyForVatTemplateAndOneGroupAmount() {
        var header = header("1", "K26TMA", TODAY, null, null, "VND", null, "0000000000");
        var missing = validator.validate(invoice(header, LINES, List.of(), SALES_TOTALS));
        assertThat(missing.withCode("EINV-REQ-003")).extracting(ValidationIssue::path).containsExactly(
                HEADER + "HDCTTChinh", GROUPS, TOTALS + "/TgTCThue", TOTALS + "/TgTThue");

        var incompleteGroup = group("KCT", null, null);
        var groupsWithoutAmount = List.of(incompleteGroup, incompleteGroup);
        assertFinding(validator.validate(invoice(HEADER_VAT, LINES, groupsWithoutAmount, VAT_TOTALS)),
                "EINV-REQ-003", ERROR, GROUPS + "[1]/ThTien");
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, List.of(incompleteGroup, GROUP.get(0)), VAT_TOTALS))
                .withCode("EINV-REQ-003")).isEmpty();
    }

    @Test
    void requiresNonTariffAreaFlagForSalesTemplateOnly() {
        var missingFlag = header("2", "K26TMA", TODAY, null, null, "VND", null, "0000000000");
        var sales = header("2", "K26TMA", TODAY, null, 0, "VND", null, "0000000000");
        var other = header("8", "K26TMA", TODAY, null, null, "VND", null, "0000000000");

        assertFinding(validator.validate(invoice(missingFlag, LINES, List.of(), SALES_TOTALS)),
                "EINV-REQ-003", ERROR, HEADER + "HDDCKPTQuan");
        assertThat(validator.validate(invoice(sales, LINES, List.of(), SALES_TOTALS)).issues()).isEmpty();
        assertThat(validator.validate(invoice(other, LINES, List.of(), SALES_TOTALS)).issues()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void codedInvoiceRequiresAuthorityCode(String missing) {
        var coded = header("1", "C26TMA", TODAY, 0, null, "VND", null, "0000000000");
        var invalid = new InvoiceDocument(coded, SELLER, null, LINES, GROUP, VAT_TOTALS, missing, null, null);
        var valid = new InvoiceDocument(coded, SELLER, null, LINES, GROUP, VAT_TOTALS, "SYNTHETIC", null, null);

        assertFinding(validator.validate(invalid), "EINV-REQ-004", ERROR, "HDon/MCCQT");
        assertThat(validator.validate(valid).issues()).isEmpty();
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, GROUP, VAT_TOTALS)).withCode("EINV-REQ-004")).isEmpty();
    }

    @Test
    void requiresExchangeRateForForeignCurrencyOnly() {
        var foreign = header("1", "K26TMA", TODAY, 0, null, "USD", null, "0000000000");
        var withRate = header("1", "K26TMA", TODAY, 0, null, "USD", "30000", "0000000000");

        assertFinding(validator.validate(invoice(foreign, LINES, GROUP, VAT_TOTALS)),
                "EINV-CUR-001", ERROR, HEADER + "TGia");
        assertThat(validator.validate(invoice(withRate, LINES, GROUP, VAT_TOTALS)).issues()).isEmpty();
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, GROUP, VAT_TOTALS)).withCode("EINV-CUR-001")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "001," + ITEMS + "/HHDVu/ThTien",
            "002," + GROUPS + "/ThTien",
            "003," + GROUPS + "/TThue",
            "004," + TOTALS + "/TgTCThue",
            "005," + TOTALS + "/TgTThue",
            "006," + TOTALS + "/TgTTTBSo"
    })
    void appliesExactComparisonAndToleranceToAllMonetaryRules(String rule, String path) {
        String code = "EINV-AMT-" + rule;
        assertThat(validator.validate(differentAmount(rule, "0.000")).withCode(code)).isEmpty();
        assertFinding(validator.validate(differentAmount(rule, "0.125")), code, INFO, path);
        assertFinding(validator.validate(differentAmount(rule, "-1.000")), code, INFO, path);
        assertFinding(validator.validate(differentAmount(rule, "1.001")), code, ERROR, path);

        var strict = InvoiceValidator.builder().clock(CLOCK).amountTolerance(BigDecimal.ZERO).build();
        var lenient = InvoiceValidator.builder().clock(CLOCK).amountTolerance(new BigDecimal("2")).build();
        assertFinding(strict.validate(differentAmount(rule, "0.125")), code, ERROR, path);
        assertFinding(lenient.validate(differentAmount(rule, "1.001")), code, INFO, path);
    }

    @Test
    void skipsArithmeticWithMissingInputs() {
        var report = validator.validate(invoice(HEADER_VAT, List.of(line(1, null, null)),
                List.of(group(null, null, null)), totals(null, null, null, null)));

        assertThat(report.issues()).extracting(ValidationIssue::code)
                .allMatch(code -> !code.startsWith("EINV-AMT-"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"KCT", "KKKNT", "KHAC", "1000%", "+5%", "5.%", "5% "})
    void skipsTaxCalculationForRatesOutsideThePercentagePattern(String rate) {
        var report = validator.validate(invoice(HEADER_VAT, List.of(line(1, "100", rate)),
                List.of(group(rate, "100", "10")), VAT_TOTALS));
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void calculatesDecimalPercentageExactlyAndMatchesTaxRatesAsWritten() {
        var summaries = List.of(group("5.125%", "100", "5.125"), group("10.0%", "0", "0"));
        var rows = List.of(line(1, "100", "5.125%"), line(1, "100", "10%"));
        var report = validator.validate(invoice(HEADER_VAT, rows, summaries, totals("100", "5.125", "105.125", null)));

        assertThat(report.withCode("EINV-AMT-003")).isEmpty();
        assertFinding(report, "EINV-AMT-007", ERROR, ITEMS + "/HHDVu[2]/TSuat");
        assertThat(validator.validate(invoice(HEADER_VAT, rows, List.of(), VAT_TOTALS)).withCode("EINV-AMT-007"))
                .isEmpty();
    }

    @Test
    void indexesTaxGroupIssuesInXmlOrder() {
        var groups = List.of(group("10%", "103", "13"), group("5%", "20", "4"));
        var report = validator.validate(invoice(HEADER_VAT, LINES, groups, VAT_TOTALS));

        assertThat(report.withCode("EINV-AMT-002")).extracting(ValidationIssue::path)
                .containsExactly(GROUPS + "[1]/ThTien", GROUPS + "[2]/ThTien");
        assertThat(report.withCode("EINV-AMT-003")).extracting(ValidationIssue::path)
                .containsExactly(GROUPS + "[1]/TThue", GROUPS + "[2]/TThue");
    }

    @Test
    void warnsAboutFractionalVndTotalsButAcceptsTrailingZerosAndForeignCurrency() {
        var fractional = totals("100.01", "10.01", "110.01", "5.01");
        var report = validator.validate(invoice(HEADER_VAT, LINES, GROUP, fractional));
        assertThat(report.warnings()).extracting(ValidationIssue::code, ValidationIssue::path).containsExactly(
                tuple("EINV-AMT-008", TOTALS + "/TgTCThue"), tuple("EINV-AMT-008", TOTALS + "/TgTThue"),
                tuple("EINV-AMT-008", TOTALS + "/TgTTTBSo"), tuple("EINV-AMT-008", TOTALS + "/TTCKTMai"));
        assertThat(report.isValid()).isTrue();
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, GROUP, totals("100.000", "10.00", "110.0", "0.00")))
                .withCode("EINV-AMT-008")).isEmpty();
        var foreign = header("1", "K26TMA", TODAY, 0, null, "USD", "30000", "0000000000");
        assertThat(validator.validate(invoice(foreign, LINES, GROUP, fractional)).withCode("EINV-AMT-008")).isEmpty();
    }

    @Test
    void unsupportedLineNaturesSuppressArithmeticOnceAndStillCheckVndTotals() {
        var rows = List.of(line(1, "500", "8%"), line(null, "300", "10%"), line(2, "400", "10%"));
        var charges = Map.of(TOTALS + "/OtherCharge", "10");
        var totals = totals("500", "100", "700.01", null);
        var document = new InvoiceDocument(HEADER_VAT, SELLER, null, rows, GROUP, totals, null, null, charges);

        assertThat(validator.validate(document).issues()).extracting(
                ValidationIssue::code, ValidationIssue::severity, ValidationIssue::path).containsExactly(
                tuple("EINV-REQ-001", ERROR, ITEMS + "/HHDVu[2]/TChat"),
                tuple("EINV-AMT-008", WARNING, TOTALS + "/TgTTTBSo"),
                tuple("EINV-AMT-900", INFO, ITEMS));
    }

    @Test
    void otherChargesSuppressOnlyPaymentComparisonAndOtherInformationDoesNot() {
        var totals = totals("100", "10", "120", null);
        var rows = List.of(line(1, "95", "10%"));
        var charges = Map.of(TOTALS + "/OtherCharge", "10", TOTALS + "/AnotherCharge", "5");
        var withCharges = new InvoiceDocument(HEADER_VAT, SELLER, null, rows, GROUP, totals, null, null, charges);
        var report = validator.validate(withCharges);
        assertThat(report.withCode("EINV-AMT-006")).isEmpty();
        assertFinding(report, "EINV-AMT-002", ERROR, GROUPS + "/ThTien");
        assertFinding(report, "EINV-AMT-900", INFO, TOTALS);

        var extras = Map.of(TOTALS + "/TTKhac/TTin", "synthetic", TOTALS + "/TTKhac[2]/TTin", "synthetic");
        var withOtherInformation =
                new InvoiceDocument(HEADER_VAT, SELLER, null, LINES, GROUP, totals, null, null, extras);
        assertFinding(validator.validate(withOtherInformation), "EINV-AMT-006", ERROR, TOTALS + "/TgTTTBSo");
        assertThat(validator.validate(withOtherInformation).withCode("EINV-AMT-900")).isEmpty();
    }

    @Test
    void checksIssueDateInTheClocksZone() {
        var document = invoice(HEADER_VAT, LINES, GROUP, VAT_TOTALS);
        var utc = InvoiceValidator.builder().clock(Clock.fixed(START_OF_DAY, ZoneOffset.UTC)).build();

        assertThat(validator.validate(document).withCode("EINV-DATE-001")).isEmpty();
        assertFinding(utc.validate(document), "EINV-DATE-001", ERROR, HEADER + "NLap");
    }

    @Test
    void checksSigningDateInVietnamRegardlessOfTheClocksZone() {
        var signatures = List.of(
                new InvoiceSignature(SignerRole.SELLER, START_OF_DAY, null),
                new InvoiceSignature(SignerRole.BUYER, START_OF_DAY.plusSeconds(60), null),
                new InvoiceSignature(SignerRole.SELLER, START_OF_DAY.minusNanos(1), null),
                new InvoiceSignature(SignerRole.TAX_AUTHORITY, null, null),
                new InvoiceSignature(null, START_OF_DAY.minusSeconds(60), null));
        var document = new InvoiceDocument(HEADER_VAT, SELLER, null, LINES, GROUP, VAT_TOTALS, null, signatures, null);
        var utc = InvoiceValidator.builder().clock(Clock.fixed(START_OF_DAY.plusSeconds(3600), ZoneOffset.UTC)).build();

        assertThat(utc.validate(document).withCode("EINV-DATE-002"))
                .extracting(ValidationIssue::severity, ValidationIssue::path).containsExactly(
                        tuple(WARNING, "HDon/DSCKS/NBan/Signature[2]" + SIGNING_TIME),
                        tuple(WARNING, "HDon/DSCKS"));
    }

    @Test
    void checksTaxCodeFormatsInSellerBuyerProviderOrderAndRedactsMessages() {
        var header = header("1", "K26TMA", TODAY, 0, null, "VND", null, "PRIVATE_PROVIDER_TAX_CODE");
        var seller = new InvoiceParty("PRIVATE_SELLER_NAME", "PRIVATE_SELLER_TAX_CODE", "PRIVATE_ADDRESS", null);
        var buyer = new InvoiceParty("PRIVATE_BUYER_NAME", "PRIVATE_BUYER_TAX_CODE", null, null);
        var rows = List.of(line(1, "798234.1539", "10%"));
        var document = new InvoiceDocument(header, seller, buyer, rows, GROUP, VAT_TOTALS, null, null, null);
        var report = validator.validate(document);

        assertThat(report.withCode("EINV-MST-001")).extracting(ValidationIssue::severity, ValidationIssue::path)
                .containsExactly(tuple(WARNING, CONTENT + "NBan/MST"), tuple(WARNING, CONTENT + "NMua/MST"),
                        tuple(WARNING, HEADER + "MSTTCGP"));
        assertThat(report.withCode("EINV-AMT-001")).hasSize(1);
        assertThat(report.issues()).allSatisfy(issue ->
                assertThat(issue.message()).doesNotContain("PRIVATE_", "798234.1539"));
        assertThat(validator.validate(invoice(HEADER_VAT, LINES, GROUP, VAT_TOTALS)).withCode("EINV-MST-001"))
                .isEmpty();
    }

    @Test
    void appliesApplicationRulesInOrderAfterDefaultRulesAndPropagatesExceptions() {
        var document = new InvoiceDocument(null, null, null, null, null, null, null, null, null);
        var first = new ValidationIssue("FIRST", INFO, "HDon/MCCQT", "First rule.");
        var second = new ValidationIssue("SECOND", WARNING, "HDon/MCCQT", "Second rule.");
        var builder = InvoiceValidator.builder().clock(CLOCK).addRule(value -> {
            assertThat(value).isSameAs(document);
            return List.of(first);
        });
        var oneRule = builder.build();
        var twoRules = builder.addRule(value -> List.of(second)).build();

        assertThat(oneRule.validate(document).issues()).endsWith(first);
        var report = twoRules.validate(document);
        assertThat(report.issues()).endsWith(report.withCode("EINV-REQ-002").get(0), first, second);
        assertThat(report.warnings()).containsExactly(second);
        var failure = new IllegalStateException("application failure");
        var failing = builder.addRule(value -> { throw failure; }).build();
        assertThatThrownBy(() -> failing.validate(document)).isSameAs(failure);
    }

    @Test
    void rejectsInvalidBuilderSettingsAtBuildAndNullDocumentsAtValidation() {
        var noClock = InvoiceValidator.builder().clock(null);
        var noRule = InvoiceValidator.builder().clock(CLOCK).addRule(null);
        var noTolerance = InvoiceValidator.builder().clock(CLOCK).amountTolerance(null);
        var negative = InvoiceValidator.builder().clock(CLOCK).amountTolerance(new BigDecimal("-0.01"));

        assertThatThrownBy(noClock::build).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(noRule::build).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(noTolerance::build).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(negative::build).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void reportCopiesIssuesAndRejectsNullIssueFields() {
        var error = new ValidationIssue("EXAMPLE", ERROR, "HDon/MCCQT", "Example error.");
        var warning = new ValidationIssue("EXAMPLE", WARNING, "HDon/MCCQT", "Example warning.");
        var findings = new ArrayList<>(List.of(error, warning));
        var report = new ValidationReport(findings);

        findings.clear();
        assertThat(report.withCode("EXAMPLE")).containsExactly(error, warning);
        assertThat(report.errors()).containsExactly(error);
        assertThat(report.warnings()).containsExactly(warning);
        assertThat(report.isValid()).isFalse();
        assertThatThrownBy(() -> report.issues().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ValidationIssue(null, INFO, "HDon/MCCQT", "Issue."))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ValidationIssue("EXAMPLE", null, "HDon/MCCQT", "Issue."))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ValidationIssue("EXAMPLE", INFO, null, "Issue."))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ValidationIssue("EXAMPLE", INFO, "HDon/MCCQT", null))
                .isInstanceOf(NullPointerException.class);
    }

    private static void assertFinding(ValidationReport report, String code, Severity severity, String path) {
        assertThat(report.withCode(code)).extracting(ValidationIssue::severity, ValidationIssue::path)
                .containsExactly(tuple(severity, path));
    }

    private static InvoiceDocument differentAmount(String rule, String difference) {
        return switch (rule) {
            case "001" -> invoice(HEADER_VAT, List.of(line(1, plus("100", difference), "10%")), GROUP, VAT_TOTALS);
            case "002" -> invoice(HEADER_VAT, LINES, List.of(group("10%", plus("100", difference), "10")), VAT_TOTALS);
            case "003" -> invoice(HEADER_VAT, LINES, List.of(group("10%", "100", plus("10", difference))), VAT_TOTALS);
            case "004" -> invoice(HEADER_VAT, LINES, GROUP, totals(plus("100", difference), "10", "110", null));
            case "005" -> invoice(HEADER_VAT, LINES, GROUP, totals("100", plus("10", difference), "110", null));
            case "006" -> invoice(HEADER_VAT, LINES, GROUP, totals("100", "10", plus("110", difference), null));
            default -> throw new IllegalArgumentException("Unknown test rule");
        };
    }

    private static InvoiceDocument invoice(InvoiceHeader header, List<InvoiceItem> items,
                                           List<TaxRateSummary> groups, InvoiceTotals totals) {
        return new InvoiceDocument(header, SELLER, null, items, groups, totals, null, null, null);
    }

    private static InvoiceHeader header(String template, String symbol, LocalDate date, Integer leasing,
                                         Integer nonTariff, String currency, String exchangeRate, String providerTaxCode) {
        return new InvoiceHeader("2.1.1", "Sample invoice", template, symbol, null, date, leasing, nonTariff,
                currency, decimal(exchangeRate), "Transfer", providerTaxCode);
    }

    private static InvoiceItem line(Integer nature, String amount, String rate) {
        return new InvoiceItem(nature, null, "Sample item", null, decimal("2.0"), decimal("55.00"),
                decimal(amount), decimal("10"), decimal("10.00"), rate);
    }

    private static TaxRateSummary group(String rate, String amount, String tax) {
        return new TaxRateSummary(rate, decimal(amount), decimal(tax));
    }

    private static InvoiceTotals totals(String beforeTax, String tax, String payment, String tradeDiscount) {
        return new InvoiceTotals(decimal(beforeTax), decimal(tax), decimal(tradeDiscount), decimal(payment),
                "Sample words");
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static String plus(String amount, String difference) {
        return decimal(amount).add(decimal(difference)).toPlainString();
    }
}
