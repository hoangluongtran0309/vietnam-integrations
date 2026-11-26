package io.github.hoangluongtran0309.vietnam.einvoice.signature;

/** Result of checking one invoice XML signature. */
public enum SignatureStatus {
    /** The signature is cryptographically valid and covers the invoice data. */
    VALID,
    /** The signature value could not be verified with the embedded certificate. */
    INVALID_SIGNATURE,
    /** A reference is forbidden, cannot be resolved, or failed digest validation. */
    INVALID_REFERENCE,
    /** The signature is cryptographically valid but does not cover the invoice data. */
    REFERENCE_NOT_COVERING_INVOICE,
    /** A signature, digest, canonicalization, or transform algorithm is outside the allowlist. */
    UNSUPPORTED_ALGORITHM,
    /** The signature contains no X.509 signing certificate. */
    MISSING_CERTIFICATE,
    /** The claimed signing time is outside the signing certificate's validity period. */
    CERTIFICATE_NOT_VALID_AT_SIGNING_TIME,
    /** The signature cannot be read or validated as XMLDSig. */
    MALFORMED
}
