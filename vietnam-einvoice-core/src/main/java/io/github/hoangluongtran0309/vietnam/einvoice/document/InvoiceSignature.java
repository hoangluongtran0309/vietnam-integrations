package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.time.Instant;

/**
 * Metadata extracted from a signature without verification state or signature bytes.
 *
 * @param role signer role derived from {@code HDon/DSCKS/NBan/ds:Signature},
 *             {@code HDon/DSCKS/NMua/ds:Signature}, or {@code HDon/DSCKS/CQT/ds:Signature}; may be {@code null}
 *             when the signer container cannot be identified
 * @param signingTime value at
 *                    {@code HDon/DSCKS/NBan/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime},
 *                    {@code HDon/DSCKS/NMua/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime},
 *                    or
 *                    {@code HDon/DSCKS/CQT/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime};
 *                    may be {@code null} when absent
 * @param certificate metadata from the certificate at
 *                    {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                    {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                    {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                    {@code null} when absent
 */
public record InvoiceSignature(SignerRole role, Instant signingTime, CertificateInfo certificate) {
}
