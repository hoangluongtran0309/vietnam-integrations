package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

final class CertificateTrust {

    /** Caps work per path at ten certificates, including the leaf and excluding the trust anchor. */
    private static final int MAX_PATH_LENGTH = 10;

    /** Bounds certificate signature checks during path expansion when a signer supplies many candidate issuers. */
    private static final int MAX_PATH_EXPANSIONS = 1_000;

    private CertificateTrust() {
    }

    static boolean validates(List<X509Certificate> embedded, Collection<X509Certificate> intermediates,
                             Set<TrustAnchor> anchors, Instant signingTime) {
        try {
            var parameters = new PKIXParameters(anchors);
            parameters.setRevocationEnabled(false);
            parameters.setDate(Date.from(signingTime));
            var validator = CertPathValidator.getInstance("PKIX", "SUN");
            var certificates = CertificateFactory.getInstance("X.509", "SUN");

            var candidates = new ArrayList<>(intermediates);
            candidates.addAll(embedded.subList(1, embedded.size()));
            var bySubject = new HashMap<X500Principal, Map<PublicKey, X509Certificate>>();
            for (X509Certificate candidate : candidates) {
                if (candidate.getBasicConstraints() >= 0) {
                    // Prefer an application-supplied certificate over an embedded variant of the same issuer.
                    bySubject.computeIfAbsent(candidate.getSubjectX500Principal(), ignored -> new LinkedHashMap<>())
                            .putIfAbsent(candidate.getPublicKey(), candidate);
                }
            }

            // Assemble candidate paths in memory. The JDK PKIX builder can fetch AIA issuers
            // when the hosting JVM enables that feature.
            var pending = new ArrayDeque<List<X509Certificate>>();
            pending.push(List.of(embedded.get(0)));
            int remainingExpansions = MAX_PATH_EXPANSIONS;
            while (!pending.isEmpty()) {
                List<X509Certificate> path = pending.pop();
                X509Certificate last = path.get(path.size() - 1);
                for (TrustAnchor anchor : anchors) {
                    X509Certificate anchorCertificate = anchor.getTrustedCert();
                    if (last.equals(anchorCertificate)
                            && validatesPath(validator, certificates, parameters, anchor,
                                    path.subList(0, path.size() - 1))) {
                        return true;
                    }
                    X500Principal anchorSubject = anchorCertificate == null ? anchor.getCA()
                            : anchorCertificate.getSubjectX500Principal();
                    if (last.getIssuerX500Principal().equals(anchorSubject)
                            && validatesPath(validator, certificates, parameters, anchor, path)) {
                        return true;
                    }
                }
                if (path.size() == MAX_PATH_LENGTH) {
                    continue;
                }
                var issuers = bySubject.getOrDefault(last.getIssuerX500Principal(), Map.of()).values();
                for (X509Certificate issuer : issuers) {
                    if (path.contains(issuer)) {
                        continue;
                    }
                    if (remainingExpansions == 0) {
                        return false;
                    }
                    remainingExpansions--;
                    if (issuedBy(last, issuer)) {
                        var extended = new ArrayList<>(path);
                        extended.add(issuer);
                        pending.push(extended);
                    }
                }
            }
            return false;
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private static boolean validatesPath(CertPathValidator validator, CertificateFactory certificates,
                                         PKIXParameters parameters, TrustAnchor anchor,
                                         List<X509Certificate> path) {
        try {
            parameters.setTrustAnchors(Set.of(anchor));
            validator.validate(certificates.generateCertPath(path), parameters);
            return true;
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private static boolean issuedBy(X509Certificate certificate, X509Certificate issuer) {
        try {
            certificate.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }
}
