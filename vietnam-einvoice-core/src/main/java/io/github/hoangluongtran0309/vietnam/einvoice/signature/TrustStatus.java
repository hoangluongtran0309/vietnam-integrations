package io.github.hoangluongtran0309.vietnam.einvoice.signature;

/** Result of checking trust in an invoice signing certificate. */
public enum TrustStatus {
    /** No trust anchors were supplied, or the signature status is other than VALID. */
    NOT_CHECKED,
    /** An offline PKIX path validated at the claimed signing time. */
    TRUSTED,
    /** No offline PKIX path could be validated within the search limits, or the signing time is absent. */
    UNTRUSTED
}
