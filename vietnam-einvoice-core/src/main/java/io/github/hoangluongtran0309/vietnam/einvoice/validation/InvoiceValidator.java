package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates the structure and consistency of a parsed invoice.
 *
 * <p>Amount tolerance is an application policy. The sources reviewed in
 * {@code docs/einvoice/legal-basis.md} do not prescribe a general rounding method.
 * Exact matches produce no issue, nonzero differences within tolerance produce an information
 * issue, and differences exceeding tolerance produce an error.</p>
 */
public final class InvoiceValidator {

    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;
    private final BigDecimal amountTolerance;
    private final List<InvoiceRule> rules;

    private InvoiceValidator(Builder builder) {
        clock = Objects.requireNonNull(builder.clock, "clock");
        amountTolerance = Objects.requireNonNull(builder.amountTolerance, "amountTolerance");
        if (amountTolerance.signum() < 0) {
            throw new IllegalArgumentException("amountTolerance must be non-negative");
        }
        rules = List.copyOf(builder.rules);
    }

    /** Returns a validator using the current time in Vietnam and an amount tolerance of one. */
    public static InvoiceValidator defaults() {
        return builder().build();
    }

    /** Returns a builder initialized with the default clock and amount tolerance. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Applies the default rules, then each application rule in registration order.
     * Exceptions thrown by application rules propagate to the caller.
     *
     * @throws NullPointerException if the document is null
     */
    public ValidationReport validate(InvoiceDocument document) {
        Objects.requireNonNull(document, "document");
        var issues = new ArrayList<ValidationIssue>();
        new DefaultInvoiceRules(document, clock, amountTolerance, issues).check();
        for (InvoiceRule rule : rules) {
            issues.addAll(rule.check(document));
        }
        return new ValidationReport(issues);
    }

    /** Configures the default rules and appends application rules. */
    public static final class Builder {

        private Clock clock = Clock.system(VIETNAM);
        private BigDecimal amountTolerance = BigDecimal.ONE;
        private final List<InvoiceRule> rules = new ArrayList<>();

        private Builder() {
        }

        /** Sets the clock used to determine the current date. */
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /** Sets the maximum absolute amount difference treated as informational. */
        public Builder amountTolerance(BigDecimal tolerance) {
            amountTolerance = tolerance;
            return this;
        }

        /** Appends a rule to apply after the default rules. */
        public Builder addRule(InvoiceRule rule) {
            rules.add(rule);
            return this;
        }

        /** Builds a validator and rejects null settings, null rules, and negative tolerance. */
        public InvoiceValidator build() {
            return new InvoiceValidator(this);
        }
    }
}
