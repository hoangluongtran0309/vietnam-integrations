package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import java.util.List;

/** Immutable findings in validation order. */
public record ValidationReport(List<ValidationIssue> issues) {

    public ValidationReport {
        issues = List.copyOf(issues);
    }

    /** Returns whether validation found no errors. */
    public boolean isValid() {
        return issues.stream().noneMatch(issue -> issue.severity() == Severity.ERROR);
    }

    /** Returns the error findings in validation order. */
    public List<ValidationIssue> errors() {
        return issues.stream().filter(issue -> issue.severity() == Severity.ERROR).toList();
    }

    /** Returns the warning findings in validation order. */
    public List<ValidationIssue> warnings() {
        return issues.stream().filter(issue -> issue.severity() == Severity.WARNING).toList();
    }

    /** Returns findings with the given rule code in validation order. */
    public List<ValidationIssue> withCode(String code) {
        return issues.stream().filter(issue -> issue.code().equals(code)).toList();
    }
}
