package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.CertificateInfo;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;

import java.time.Instant;

/**
 * Verification result for one invoice signature.
 *
 * @param role the signer container's role
 * @param path the signature element's local-name path with 1-based indices for repeated siblings
 * @param status the signature verification status
 * @param trust the certificate trust status
 * @param signatureAlgorithm the SignatureMethod URI, or {@code null} if unavailable
 * @param signingTime the claimed signing time, or {@code null} if unavailable
 * @param certificate metadata from the signing certificate, or {@code null} if unavailable
 * @param coversInvoiceData whether a verified reference covers the direct invoice data element
 * @param coversTaxAuthorityCode whether a verified reference covers the direct tax-authority code element
 */
public record SignatureCheck(
        SignerRole role,
        String path,
        SignatureStatus status,
        TrustStatus trust,
        String signatureAlgorithm,
        Instant signingTime,
        CertificateInfo certificate,
        boolean coversInvoiceData,
        boolean coversTaxAuthorityCode) {
}
