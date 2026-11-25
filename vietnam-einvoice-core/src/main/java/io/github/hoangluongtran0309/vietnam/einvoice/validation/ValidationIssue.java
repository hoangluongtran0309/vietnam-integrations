package io.github.hoangluongtran0309.vietnam.einvoice.validation;

import java.util.Objects;

/**
 * A finding produced by an invoice rule.
 *
 * @param code stable rule identifier
 * @param severity impact of the finding
 * @param path XML path rooted at {@code HDon}, with 1-based indices for repeated siblings
 * @param message English description without invoice values
 */
public record ValidationIssue(String code, Severity severity, String path, String message) {

    public ValidationIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(message, "message");
    }
}
