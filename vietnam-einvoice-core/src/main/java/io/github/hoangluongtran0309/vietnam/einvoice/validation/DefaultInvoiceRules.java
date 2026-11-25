package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceItem;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceTotals;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.document.TaxRateSummary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.ERROR;
import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.INFO;
import static io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity.WARNING;

final class DefaultInvoiceRules {

    private static final String HEADER = "HDon/DLHDon/TTChung";
    private static final String CONTENT = "HDon/DLHDon/NDHDon";
    private static final String SELLER = CONTENT + "/NBan";
    private static final String BUYER = CONTENT + "/NMua";
    private static final String ITEMS = CONTENT + "/DSHHDVu";
    private static final String TOTALS = CONTENT + "/TToan";
    private static final String SUMMARIES = TOTALS + "/THTTLTSuat";
    private static final String SIGNATURES = "HDon/DSCKS";
    private static final String SIGNING_TIME =
            "/Object/QualifyingProperties/SignedProperties/SignedSignatureProperties/SigningTime";
    private static final Pattern OTHER_INFORMATION = Pattern.compile(TOTALS + "/TTKhac(?:\\[\\d+\\])?/.*");
    private static final Pattern PERCENT = Pattern.compile("\\d{1,3}(?:\\.\\d+)?%");
    private static final Pattern TAX_CODE = Pattern.compile("\\d{10}(?:-\\d{3})?");

    private final InvoiceDocument invoice;
    private final InvoiceHeader header;
    private final InvoiceTotals totals;
    private final List<InvoiceItem> items;
    private final List<TaxRateSummary> summaries;
    private final Clock clock;
    private final BigDecimal tolerance;
    private final List<ValidationIssue> issues;

    DefaultInvoiceRules(InvoiceDocument invoice, Clock clock, BigDecimal tolerance, List<ValidationIssue> issues) {
        this.invoice = invoice;
        this.header = invoice.header();
        this.totals = invoice.totals();
        this.items = invoice.items();
        this.summaries = invoice.taxRateSummaries();
        this.clock = clock;
        this.tolerance = tolerance;
        this.issues = issues;
    }

    void check() {
        checkRequiredFields();
        if (items.isEmpty()) {
            add("EINV-REQ-002", ERROR, ITEMS, "At least one invoice line is required.");
        }
        checkRequiredByInvoiceType();
        checkAuthorityCode();
        if (header != null && !missing(header.currencyCode()) && !"VND".equals(header.currencyCode())
                && header.exchangeRate() == null) {
            add("EINV-CUR-001", ERROR, HEADER + "/TGia", "An exchange rate is required for foreign currency.");
        }
        checkAmounts();
        checkDates();
        checkTaxCodes();
    }

    private void checkRequiredFields() {
        required(header, InvoiceHeader::formatVersion, HEADER + "/PBan");
        required(header, InvoiceHeader::invoiceName, HEADER + "/THDon");
        required(header, InvoiceHeader::invoiceTemplateSymbol, HEADER + "/KHMSHDon");
        required(header, InvoiceHeader::invoiceSymbol, HEADER + "/KHHDon");
        required(header, InvoiceHeader::issueDate, HEADER + "/NLap");
        required(header, InvoiceHeader::currencyCode, HEADER + "/DVTTe");
        required(header, InvoiceHeader::paymentMethod, HEADER + "/HTTToan");
        required(header, InvoiceHeader::solutionProviderTaxCode, HEADER + "/MSTTCGP");
        required(invoice.seller(), InvoiceParty::name, SELLER + "/Ten");
        required(invoice.seller(), InvoiceParty::taxCode, SELLER + "/MST");
        required(invoice.seller(), InvoiceParty::address, SELLER + "/DChi");
        for (int i = 0; i < items.size(); i++) {
            String path = itemPath(i);
            required(items.get(i), InvoiceItem::nature, path + "/TChat");
            required(items.get(i), InvoiceItem::name, path + "/THHDVu");
        }
        required(totals, InvoiceTotals::totalPaymentAmount, TOTALS + "/TgTTTBSo");
        required(totals, InvoiceTotals::totalPaymentAmountInWords, TOTALS + "/TgTTTBChu");
    }

    private void checkRequiredByInvoiceType() {
        if (header == null) {
            return;
        }
        // Circular 91/2026/TT-BTC, Appendix I, section 1.
        if ("1".equals(header.invoiceTemplateSymbol())) {
            requiredByType(header.financialLeasingFlag(), HEADER + "/HDCTTChinh");
            if (summaries.isEmpty()) {
                requiredByType(null, SUMMARIES + "/LTSuat");
            } else if (summaries.stream().noneMatch(summary -> summary.amount() != null)) {
                // Only one aggregate is required to have an amount. Point to the first candidate.
                requiredByType(null, summaryPath(0) + "/ThTien");
            }
            requiredByType(value(totals, InvoiceTotals::totalAmountBeforeTax), TOTALS + "/TgTCThue");
            requiredByType(value(totals, InvoiceTotals::totalTaxAmount), TOTALS + "/TgTThue");
        } else if ("2".equals(header.invoiceTemplateSymbol())) {
            requiredByType(header.nonTariffAreaFlag(), HEADER + "/HDDCKPTQuan");
        }
    }

    private void checkAuthorityCode() {
        // Circular 91/2026/TT-BTC, Appendix I, section 2.
        String symbol = value(header, InvoiceHeader::invoiceSymbol);
        if (symbol != null && symbol.startsWith("C") && missing(invoice.taxAuthorityCode())) {
            add("EINV-REQ-004", ERROR, "HDon/MCCQT", "A tax authority code is required for a coded invoice.");
        }
    }

    private <T> void required(T parent, Function<T, ?> field, String path) {
        if (missing(value(parent, field))) {
            add("EINV-REQ-001", ERROR, path, "Required field is missing.");
        }
    }

    private void requiredByType(Object field, String path) {
        if (missing(field)) {
            add("EINV-REQ-003", ERROR, path, "Required field for this invoice type is missing.");
        }
    }

    private void checkAmounts() {
        // TODO(v0.3): verify TChat values and how promotion, discount and note lines affect totals — official QD 1233/QD-CT.
        boolean unsupportedLine = items.stream().anyMatch(item -> !Integer.valueOf(1).equals(item.nature()));
        boolean unmodeledCharges = false;
        if (!unsupportedLine) {
            checkLineAmounts();
            checkAmountsByTaxRate();
            checkTaxesByTaxRate();
            checkTotalsAgainstTaxGroups();
            unmodeledCharges = checkTotalPayment();
            checkTaxGroupMembership();
        }
        checkVndTotals();
        if (unsupportedLine) {
            add("EINV-AMT-900", INFO, ITEMS, "Arithmetic checks were skipped because a line has an unsupported nature.");
        } else if (unmodeledCharges) {
            add("EINV-AMT-900", INFO, TOTALS, "The payment total check was skipped because charges may be unmodeled.");
        }
    }

    private void checkLineAmounts() {
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            if (item.quantity() != null && item.unitPrice() != null && item.amount() != null) {
                BigDecimal discount = item.discountAmount() == null ? BigDecimal.ZERO : item.discountAmount();
                BigDecimal expected = item.quantity().multiply(item.unitPrice()).subtract(discount);
                compare("EINV-AMT-001", itemPath(i) + "/ThTien", expected, item.amount(),
                        "Line amount differs from quantity times unit price less the line discount.");
            }
        }
    }

    private void checkAmountsByTaxRate() {
        if (items.isEmpty() || summaries.isEmpty() || items.stream().anyMatch(item -> missing(item.taxRate()))) {
            return;
        }
        Map<String, BigDecimal> amounts = new HashMap<>();
        Set<String> incompleteRates = new HashSet<>();
        for (InvoiceItem item : items) {
            if (item.amount() == null) {
                incompleteRates.add(item.taxRate());
            } else {
                amounts.merge(item.taxRate(), item.amount(), BigDecimal::add);
            }
        }
        for (int i = 0; i < summaries.size(); i++) {
            TaxRateSummary summary = summaries.get(i);
            String rate = summary.taxRate();
            if (!missing(rate) && summary.amount() != null && !incompleteRates.contains(rate)) {
                compare("EINV-AMT-002", summaryPath(i) + "/ThTien",
                        amounts.getOrDefault(rate, BigDecimal.ZERO), summary.amount(),
                        "Tax group amount differs from the sum of line amounts with the same tax rate.");
            }
        }
    }

    private void checkTaxesByTaxRate() {
        for (int i = 0; i < summaries.size(); i++) {
            TaxRateSummary summary = summaries.get(i);
            String rate = summary.taxRate();
            if (rate != null && PERCENT.matcher(rate).matches()
                    && summary.amount() != null && summary.taxAmount() != null) {
                BigDecimal percentage = new BigDecimal(rate.substring(0, rate.length() - 1)).movePointLeft(2);
                compare("EINV-AMT-003", summaryPath(i) + "/TThue",
                        summary.amount().multiply(percentage), summary.taxAmount(),
                        "Tax group tax differs from the group amount times its tax rate.");
            }
        }
    }

    private void checkTotalsAgainstTaxGroups() {
        if (totals == null || summaries.isEmpty()) {
            return;
        }
        compare("EINV-AMT-004", TOTALS + "/TgTCThue", sumGroups(TaxRateSummary::amount), totals.totalAmountBeforeTax(),
                "Total before tax differs from the sum of tax group amounts.");
        compare("EINV-AMT-005", TOTALS + "/TgTThue", sumGroups(TaxRateSummary::taxAmount), totals.totalTaxAmount(),
                "Total tax differs from the sum of tax group taxes.");
    }

    private BigDecimal sumGroups(Function<TaxRateSummary, BigDecimal> field) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TaxRateSummary summary : summaries) {
            BigDecimal amount = field.apply(summary);
            if (amount == null) {
                return null;
            }
            sum = sum.add(amount);
        }
        return sum;
    }

    private boolean checkTotalPayment() {
        if (totals == null || totals.totalAmountBeforeTax() == null || totals.totalTaxAmount() == null
                || totals.totalPaymentAmount() == null) {
            return false;
        }
        boolean unmodeledCharges = invoice.extensions().keySet().stream().anyMatch(path ->
                path.startsWith(TOTALS + "/") && !OTHER_INFORMATION.matcher(path).matches());
        if (!unmodeledCharges) {
            compare("EINV-AMT-006", TOTALS + "/TgTTTBSo",
                    totals.totalAmountBeforeTax().add(totals.totalTaxAmount()), totals.totalPaymentAmount(),
                    "Payment total differs from the sum of the total before tax and the total tax.");
        }
        return unmodeledCharges;
    }

    private void checkTaxGroupMembership() {
        if (summaries.isEmpty()) {
            return;
        }
        Set<String> rates = new HashSet<>();
        for (TaxRateSummary summary : summaries) {
            rates.add(summary.taxRate());
        }
        for (int i = 0; i < items.size(); i++) {
            String rate = items.get(i).taxRate();
            if (!missing(rate) && !rates.contains(rate)) {
                add("EINV-AMT-007", ERROR, itemPath(i) + "/TSuat", "Line tax rate has no matching tax group.");
            }
        }
    }

    private void checkVndTotals() {
        if (header != null && "VND".equals(header.currencyCode()) && totals != null) {
            wholeVnd(totals.totalAmountBeforeTax(), TOTALS + "/TgTCThue");
            wholeVnd(totals.totalTaxAmount(), TOTALS + "/TgTThue");
            wholeVnd(totals.totalPaymentAmount(), TOTALS + "/TgTTTBSo");
            wholeVnd(totals.totalTradeDiscount(), TOTALS + "/TTCKTMai");
        }
    }

    private void wholeVnd(BigDecimal amount, String path) {
        if (amount != null && amount.stripTrailingZeros().scale() > 0) {
            add("EINV-AMT-008", WARNING, path, "A VND total has a fractional part.");
        }
    }

    private void checkDates() {
        LocalDate issueDate = value(header, InvoiceHeader::issueDate);
        if (issueDate == null) {
            return;
        }
        if (issueDate.isAfter(LocalDate.now(clock))) {
            add("EINV-DATE-001", ERROR, HEADER + "/NLap", "Issue date is after the current date.");
        }
        var startOfIssueDate = issueDate.atStartOfDay(InvoiceValidator.VIETNAM).toInstant();
        var counts = new EnumMap<SignerRole, Integer>(SignerRole.class);
        for (var signature : invoice.signatures()) {
            if (signature.role() != null) {
                counts.merge(signature.role(), 1, Integer::sum);
            }
        }
        var indices = new EnumMap<SignerRole, Integer>(SignerRole.class);
        for (var signature : invoice.signatures()) {
            SignerRole role = signature.role();
            int index = role == null ? 0 : indices.merge(role, 1, Integer::sum) - 1;
            if (signature.signingTime() != null && signature.signingTime().isBefore(startOfIssueDate)) {
                String path = role == null ? SIGNATURES : signaturePath(role, index, counts.get(role));
                add("EINV-DATE-002", WARNING, path, "Signature date is before the issue date.");
            }
        }
    }

    private void checkTaxCodes() {
        // TODO(v0.3): verify 12-digit personal tax codes and the check-digit algorithm — official Ministry of Finance guidance.
        checkTaxCode(value(invoice.seller(), InvoiceParty::taxCode), SELLER + "/MST");
        checkTaxCode(value(invoice.buyer(), InvoiceParty::taxCode), BUYER + "/MST");
        checkTaxCode(value(header, InvoiceHeader::solutionProviderTaxCode), HEADER + "/MSTTCGP");
    }

    private void checkTaxCode(String code, String path) {
        if (!missing(code) && !TAX_CODE.matcher(code).matches()) {
            add("EINV-MST-001", WARNING, path, "Tax code does not match the supported format.");
        }
    }

    private void compare(String code, String path, BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null || actual == null) {
            return;
        }
        BigDecimal diff = expected.subtract(actual).abs();
        if (diff.signum() != 0) {
            add(code, diff.compareTo(tolerance) <= 0 ? INFO : ERROR, path, message);
        }
    }

    private void add(String code, Severity severity, String path, String message) {
        issues.add(new ValidationIssue(code, severity, path, message));
    }

    private String itemPath(int index) {
        return indexed(ITEMS + "/HHDVu", index, items.size());
    }

    private String summaryPath(int index) {
        return indexed(SUMMARIES + "/LTSuat", index, summaries.size());
    }

    private static String signaturePath(SignerRole role, int index, int count) {
        String signer = switch (role) {
            case SELLER -> "NBan";
            case BUYER -> "NMua";
            case TAX_AUTHORITY -> "CQT";
        };
        return indexed(SIGNATURES + "/" + signer + "/Signature", index, count) + SIGNING_TIME;
    }

    private static String indexed(String path, int index, int count) {
        return count > 1 ? path + "[" + (index + 1) + "]" : path;
    }

    private static boolean missing(Object field) {
        return field == null || field instanceof String text && text.isBlank();
    }

    private static <T, V> V value(T parent, Function<T, V> field) {
        return parent == null ? null : field.apply(parent);
    }
}
