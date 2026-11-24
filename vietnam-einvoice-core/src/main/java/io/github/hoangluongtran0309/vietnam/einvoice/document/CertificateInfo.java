package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.time.Instant;

/**
 * Metadata extracted from an X.509 certificate without retaining the certificate object or its encoded bytes.
 *
 * @param subjectDn subject distinguished name extracted from the certificate at
 *                  {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                  {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                  {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                  {@code null} when absent
 * @param issuerDn issuer distinguished name extracted from the certificate at
 *                 {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                 {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                 {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                 {@code null} when absent
 * @param serialNumber serial number extracted from the certificate at
 *                     {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                     {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                     {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                     {@code null} when absent
 * @param notBefore validity start extracted from the certificate at
 *                  {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                  {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                  {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                  {@code null} when absent
 * @param notAfter validity end extracted from the certificate at
 *                 {@code HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate},
 *                 {@code HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}, or
 *                 {@code HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate}; may be
 *                 {@code null} when absent
 */
public record CertificateInfo(
        String subjectDn,
        String issuerDn,
        String serialNumber,
        Instant notBefore,
        Instant notAfter) {
}
