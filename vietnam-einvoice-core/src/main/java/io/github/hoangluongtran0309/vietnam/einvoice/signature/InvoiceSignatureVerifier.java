package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.CertificateInfo;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import org.w3c.dom.Element;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureXml.dsigChildren;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureXml.requiredDsigChild;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.CERTIFICATE_NOT_VALID_AT_SIGNING_TIME;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.INVALID_REFERENCE;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.INVALID_SIGNATURE;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.MALFORMED;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.MISSING_CERTIFICATE;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.REFERENCE_NOT_COVERING_INVOICE;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.UNSUPPORTED_ALGORITHM;
import static io.github.hoangluongtran0309.vietnam.einvoice.signature.SignatureStatus.VALID;
import static io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode.INPUT_TOO_LARGE;

/** An immutable, thread-safe verifier for XML signatures on issued invoices. */
public final class InvoiceSignatureVerifier {

    private static final long DEFAULT_MAX_INPUT_BYTES = 10L * 1024 * 1024;
    private static final int DEFAULT_MAX_ELEMENTS = 100_000;
    private static final String SECURE_VALIDATION = "org.jcp.xml.dsig.secureValidation";
    // TODO(v0.3): verify whether invoices still in circulation use SHA-1 — real signed invoices before 2025.
    private static final Set<String> SIGNATURE_ALGORITHMS = Set.of(
            SignatureMethod.RSA_SHA256, SignatureMethod.RSA_SHA384, SignatureMethod.RSA_SHA512);
    private static final Set<String> DIGEST_ALGORITHMS = Set.of(
            DigestMethod.SHA256, DigestMethod.SHA384, DigestMethod.SHA512);
    private static final Set<String> CANONICALIZATION_ALGORITHMS = Set.of(
            CanonicalizationMethod.INCLUSIVE, CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
            CanonicalizationMethod.INCLUSIVE_11, CanonicalizationMethod.INCLUSIVE_11_WITH_COMMENTS,
            CanonicalizationMethod.EXCLUSIVE, CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS);

    private final long maxInputBytes;
    private final int maxElements;
    private final Set<TrustAnchor> trustAnchors;
    private final List<X509Certificate> intermediateCertificates;

    private InvoiceSignatureVerifier(Builder builder) {
        maxInputBytes = builder.maxInputBytes;
        maxElements = builder.maxElements;
        trustAnchors = builder.trustAnchors;
        intermediateCertificates = builder.intermediateCertificates;
    }

    /** @return a verifier with a 10 MiB input limit, a 100,000-element limit, and no trust anchors */
    public static InvoiceSignatureVerifier defaults() {
        return builder().build();
    }

    /** @return a builder with the default byte and element limits and no certificates or trust anchors */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Verifies the supported signatures in an invoice using only its XML bytes and configured certificates.
     * The first X.509 certificate in KeyInfo/X509Data in document order is treated as the signing certificate.
     * If the claimed signing time is absent, signing certificate validity dates are skipped. With configured
     * trust anchors, absence of that time yields {@link TrustStatus#UNTRUSTED} for an otherwise valid signature.
     * PKIX validation runs offline with revocation checking disabled. The search allows up to 10 certificates
     * per path (including the leaf and excluding the anchor) and at most 1,000 issuer checks per signature.
     * If trust cannot be established within those limits, the result is {@link TrustStatus#UNTRUSTED}.
     * The claimed time is not a trusted timestamp.
     *
     * @param xml the original invoice XML bytes
     * @return one check per recognized signature, in document order
     * @throws InvoiceXmlException if the invoice exceeds a byte or element limit, is malformed, has a forbidden
     *                             document type, has an unexpected root, or contains duplicate Id values
     * @throws IllegalStateException if the JVM configuration disables secure XML signature validation
     */
    public SignatureVerification verify(byte[] xml) {
        Objects.requireNonNull(xml, "xml");
        if (xml.length > maxInputBytes) {
            throw new InvoiceXmlException(INPUT_TOO_LARGE, null);
        }
        var document = SignatureXml.read(xml, maxElements);
        Element root = document.getDocumentElement();
        var ids = SignatureXml.registerIds(root);
        var signatures = SignatureXml.signatures(root);
        if (signatures.isEmpty()) {
            return new SignatureVerification(List.of());
        }

        XMLSignatureFactory factory = signatureFactory();
        Element invoiceData = SignatureXml.uniqueChild(root, "DLHDon");
        Element taxCode = SignatureXml.uniqueChild(root, "MCCQT");
        var dereferencer = localDereferencer(factory, root, ids);
        var checks = new ArrayList<SignatureCheck>(signatures.size());
        for (var signature : signatures) {
            checks.add(check(signature, factory, dereferencer, ids, invoiceData, taxCode));
        }
        return new SignatureVerification(checks);
    }

    private SignatureCheck check(SignatureXml.LocatedSignature signature, XMLSignatureFactory factory,
                                 URIDereferencer dereferencer, Map<String, Element> ids,
                                 Element invoiceData, Element taxCode) {
        String algorithm = null;
        Instant time = null;
        CertificateInfo certificate = null;
        TrustStatus trust = TrustStatus.NOT_CHECKED;
        Outcome outcome;
        try {
            Element element = signature.element();
            Element signedInfo = requiredDsigChild(element, "SignedInfo");
            algorithm = algorithm(requiredDsigChild(signedInfo, "SignatureMethod"));
            time = SignatureXml.signingTime(element);
            var certificates = certificates(element);
            if (!certificates.isEmpty()) {
                // TODO(v0.3): verify certificate order in X509Data — real signed invoices.
                X509Certificate leaf = certificates.get(0);
                certificate = new CertificateInfo(leaf.getSubjectX500Principal().getName(),
                        leaf.getIssuerX500Principal().getName(), leaf.getSerialNumber().toString(16),
                        leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant());
            }
            outcome = verifySignature(element, signedInfo, algorithm, certificates, factory, dereferencer,
                    ids, invoiceData, taxCode);
            if (outcome.status() == VALID && time != null
                    && (time.isBefore(certificate.notBefore()) || time.isAfter(certificate.notAfter()))) {
                outcome = new Outcome(CERTIFICATE_NOT_VALID_AT_SIGNING_TIME, outcome.invoice(), outcome.taxCode());
            }
            if (outcome.status() == VALID && !trustAnchors.isEmpty()) {
                trust = time != null && CertificateTrust.validates(certificates, intermediateCertificates,
                        trustAnchors, time) ? TrustStatus.TRUSTED : TrustStatus.UNTRUSTED;
            }
        } catch (GeneralSecurityException | MarshalException | RuntimeException ex) {
            // Unmarshalling malformed XMLDSig content can also throw unchecked exceptions.
            outcome = new Outcome(MALFORMED);
        }
        return new SignatureCheck(signature.role(), signature.path(), outcome.status(), trust,
                algorithm, time, certificate, outcome.invoice(), outcome.taxCode());
    }

    private static Outcome verifySignature(Element signature, Element signedInfo, String signatureAlgorithm,
                                           List<X509Certificate> certificates, XMLSignatureFactory factory,
                                           URIDereferencer dereferencer, Map<String, Element> ids,
                                           Element invoiceData, Element taxCode) throws MarshalException {
        var references = dsigChildren(signedInfo, "Reference");
        requiredDsigChild(signature, "SignatureValue");
        if (references.isEmpty()) {
            return new Outcome(MALFORMED);
        }
        if (!allowedAlgorithms(signedInfo, signatureAlgorithm, references)) {
            return new Outcome(UNSUPPORTED_ALGORITHM);
        }
        for (var reference : references) {
            var uri = reference.getAttributeNodeNS(null, "URI");
            if (uri == null || !localUri(uri.getValue(), ids)) {
                return new Outcome(INVALID_REFERENCE);
            }
        }
        if (certificates.isEmpty()) {
            return new Outcome(MISSING_CERTIFICATE);
        }

        var selector = KeySelector.singletonKeySelector(certificates.get(0).getPublicKey());
        var context = new DOMValidateContext(selector, signature);
        context.setProperty(SECURE_VALIDATION, Boolean.TRUE);
        context.setURIDereferencer(dereferencer);
        var xmlSignature = factory.unmarshalXMLSignature(context);

        try {
            if (!xmlSignature.getSignatureValue().validate(context)) {
                return new Outcome(INVALID_SIGNATURE);
            }
        } catch (XMLSignatureException ex) {
            return new Outcome(ex.getCause() instanceof SignatureException || ex.getCause() instanceof InvalidKeyException
                    ? INVALID_SIGNATURE : MALFORMED);
        }

        boolean invoiceCovered = false;
        boolean taxCodeCovered = false;
        for (Reference reference : xmlSignature.getSignedInfo().getReferences()) {
            try {
                if (!reference.validate(context)) {
                    return new Outcome(INVALID_REFERENCE);
                }
            } catch (XMLSignatureException ex) {
                return new Outcome(INVALID_REFERENCE);
            }
            if (reference.getURI().isEmpty()) {
                if (reference.getTransforms().stream().anyMatch(t -> Transform.ENVELOPED.equals(t.getAlgorithm()))) {
                    invoiceCovered |= invoiceData != null;
                    taxCodeCovered |= taxCode != null;
                }
            } else {
                Element target = ids.get(reference.getURI().substring(1));
                invoiceCovered |= invoiceData != null && invoiceData == target;
                taxCodeCovered |= taxCode != null && taxCode == target;
            }
        }
        return new Outcome(invoiceCovered ? VALID : REFERENCE_NOT_COVERING_INVOICE, invoiceCovered, taxCodeCovered);
    }

    private static boolean allowedAlgorithms(Element signedInfo, String signatureAlgorithm, List<Element> references) {
        if (!SIGNATURE_ALGORITHMS.contains(signatureAlgorithm)
                || !CANONICALIZATION_ALGORITHMS.contains(algorithm(requiredDsigChild(signedInfo,
                        "CanonicalizationMethod")))) {
            return false;
        }
        for (Element reference : references) {
            if (!DIGEST_ALGORITHMS.contains(algorithm(requiredDsigChild(reference, "DigestMethod")))) {
                return false;
            }
            for (Element transforms : dsigChildren(reference, "Transforms")) {
                for (Element transform : dsigChildren(transforms, "Transform")) {
                    String name = algorithm(transform);
                    if (!Transform.ENVELOPED.equals(name) && !CANONICALIZATION_ALGORITHMS.contains(name)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static String algorithm(Element method) {
        var attribute = method.getAttributeNodeNS(null, "Algorithm");
        if (attribute == null || attribute.getValue().isEmpty()) {
            throw new IllegalArgumentException("Missing XMLDSig algorithm");
        }
        return attribute.getValue();
    }

    private static boolean localUri(String uri, Map<String, Element> ids) {
        return uri != null && (uri.isEmpty()
                || (uri.length() > 1 && uri.charAt(0) == '#' && !uri.startsWith("#xpointer(")
                && ids.containsKey(uri.substring(1))));
    }

    private static URIDereferencer localDereferencer(XMLSignatureFactory factory, Element root,
                                                     Map<String, Element> ids) {
        URIDereferencer jdkResolver = factory.getURIDereferencer();
        return (reference, context) -> {
            String uri = reference.getURI();
            if (!localUri(uri, ids) || (!uri.isEmpty()
                    && root.getOwnerDocument().getElementById(uri.substring(1)) != ids.get(uri.substring(1)))) {
                throw new URIReferenceException("Expected a reference to this invoice XML");
            }
            // Keep the JDK's XMLDSig same-document canonicalization and comment handling.
            return jdkResolver.dereference(reference, context);
        };
    }

    private static List<X509Certificate> certificates(Element signature) throws GeneralSecurityException {
        var certificates = new ArrayList<X509Certificate>();
        var factory = CertificateFactory.getInstance("X.509", "SUN");
        for (Element keyInfo : dsigChildren(signature, "KeyInfo")) {
            for (Element data : dsigChildren(keyInfo, "X509Data")) {
                for (Element entry : dsigChildren(data, "X509Certificate")) {
                    byte[] bytes = Base64.getMimeDecoder().decode(entry.getTextContent());
                    certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
                }
            }
        }
        return certificates;
    }

    private static XMLSignatureFactory signatureFactory() {
        if (!Boolean.parseBoolean(System.getProperty(SECURE_VALIDATION, "true"))) {
            throw new IllegalStateException("Secure XML signature validation is required");
        }
        try {
            return XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException ex) {
            throw new IllegalStateException("The JDK XMLDSig provider is required", ex);
        }
    }

    /** Configures the input limits and offline certificate trust. */
    public static final class Builder {

        private long maxInputBytes = DEFAULT_MAX_INPUT_BYTES;
        private int maxElements = DEFAULT_MAX_ELEMENTS;
        private Set<TrustAnchor> trustAnchors = Set.of();
        private List<X509Certificate> intermediateCertificates = List.of();

        private Builder() {
        }

        /**
         * @param maxInputBytes positive XML byte limit, including any BOM
         * @return this builder
         * @throws IllegalArgumentException if the limit is not positive
         */
        public Builder maxInputBytes(long maxInputBytes) {
            if (maxInputBytes <= 0) {
                throw new IllegalArgumentException("maxInputBytes must be positive");
            }
            this.maxInputBytes = maxInputBytes;
            return this;
        }

        /**
         * Sets the maximum number of XML elements allowed before DOM construction, including the document root.
         * The default limit is 100,000 elements.
         *
         * @param maxElements positive element limit
         * @return this builder
         * @throws IllegalArgumentException if the limit is not positive
         */
        public Builder maxElements(int maxElements) {
            if (maxElements <= 0) {
                throw new IllegalArgumentException("maxElements must be positive");
            }
            this.maxElements = maxElements;
            return this;
        }

        /**
         * @param trustAnchors trust anchors used in offline PKIX validation
         * @return this builder
         */
        public Builder trustAnchors(Set<TrustAnchor> trustAnchors) {
            this.trustAnchors = Set.copyOf(trustAnchors);
            return this;
        }

        /**
         * @param intermediateCertificates additional issuer certificates used in offline PKIX validation
         * @return this builder
         */
        public Builder intermediateCertificates(Collection<X509Certificate> intermediateCertificates) {
            this.intermediateCertificates = List.copyOf(intermediateCertificates);
            return this;
        }

        /** @return a verifier using the configured limits and certificates */
        public InvoiceSignatureVerifier build() {
            return new InvoiceSignatureVerifier(this);
        }
    }

    private record Outcome(SignatureStatus status, boolean invoice, boolean taxCode) {
        private Outcome(SignatureStatus status) {
            this(status, false, false);
        }
    }
}
