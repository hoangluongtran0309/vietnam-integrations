package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceSignature;
import io.github.hoangluongtran0309.vietnam.einvoice.document.SignerRole;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceSignatureVerifierTest {

    private static final String XADES = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String SIGNED_PROPERTIES = "http://uri.etsi.org/01903#SignedProperties";
    private static final String STORE_PASSWORD = "test-only";
    private static final String CA_START = "2019/01/01 00:00:00";
    private static final String LEAF_START = "2022/01/01 00:00:00";
    private static final Instant SIGNING_TIME = Instant.parse("2024-07-01T02:00:00Z");
    private static final byte[] EXTERNAL_CONTENT = "external fixture".getBytes(StandardCharsets.UTF_8);
    private static final InvoiceSignatureVerifier VERIFIER = InvoiceSignatureVerifier.defaults();

    @TempDir
    static Path temp;

    private static SigningIdentity seller;
    private static SigningIdentity expired;
    private static X509Certificate root;
    private static X509Certificate unrelatedRoot;
    private static X509Certificate intermediate;
    private static List<X509Certificate> repeatedIssuers;

    @BeforeAll
    static void generateCertificates() throws Exception {
        generateKey("root", CA_START, 7300, true);
        generateKey("other", CA_START, 7300, true);
        generateKey("issuer", CA_START, 7300, true);
        generateKey("seller", LEAF_START, 3650, false);
        generateKey("expired", CA_START, 30, false);
        root = (X509Certificate) keyStore("root").getCertificate("root");
        unrelatedRoot = (X509Certificate) keyStore("other").getCertificate("other");
        intermediate = issue("issuer", "root", CA_START, 7300, true);
        seller = identity("seller", issue("seller", "issuer", LEAF_START, 3650, false));
        expired = identity("expired", issue("expired", "issuer", CA_START, 30, false));

        Path request = certificateRequest("issuer");
        repeatedIssuers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            repeatedIssuers.add(issue(request, "issuer", "repeated-issuer-" + i, CA_START, 7300, true));
        }
    }

    @Test
    void verifiesSellerSignatureAndMetadata() throws Exception {
        Document doc = sellerInvoice();
        SignatureCheck check = check(doc, VERIFIER);
        assertEquals(SignerRole.SELLER, check.role());
        assertEquals("HDon/DSCKS/NBan/Signature", check.path());
        assertEquals(SignatureStatus.VALID, check.status());
        assertEquals(TrustStatus.NOT_CHECKED, check.trust());
        assertEquals(SignatureMethod.RSA_SHA256, check.signatureAlgorithm());
        assertEquals(SIGNING_TIME, check.signingTime());
        assertEquals(InvoiceXmlParser.defaults().parse(serialize(doc)).signatures().get(0).certificate(),
                check.certificate());
        assertTrue(check.coversInvoiceData());
        assertFalse(check.coversTaxAuthorityCode());
    }

    @Test
    void reportsChangedInvoiceDataAsInvalidReference() throws Exception {
        Document doc = sellerInvoice();
        ((Element) doc.getElementsByTagName("KHHDon").item(0)).setTextContent("K24TST");
        assertEquals(SignatureStatus.INVALID_REFERENCE, check(doc, VERIFIER).status());
    }

    @Test
    void verifiesTheSignedPropertiesReference() throws Exception {
        Document doc = sellerInvoice();
        doc.getElementsByTagNameNS(XADES, "SigningTime").item(0).setTextContent("2040-07-01T02:00:00Z");
        assertEquals(SignatureStatus.INVALID_REFERENCE, check(doc, VERIFIER).status());
    }

    @Test
    void usesTheFirstCertificateEvenIfALaterOneCouldVerify() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(unrelatedRoot, seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        assertEquals(SignatureStatus.INVALID_SIGNATURE, check(doc, VERIFIER).status());
    }

    @Test
    void rejectsSha1BeforeJdkValidation() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA1, "#invoice");
        assertEquals(SignatureStatus.UNSUPPORTED_ALGORITHM, check(doc, VERIFIER).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DigestMethod", "CanonicalizationMethod"})
    void rejectsOtherAlgorithmsInSignedInfo(String method) throws Exception {
        Document doc = sellerInvoice();
        setAlgorithm(doc, method, DigestMethod.SHA1);
        assertEquals(SignatureStatus.UNSUPPORTED_ALGORITHM, check(doc, VERIFIER).status());
    }

    @Test
    void rejectsTransformOutsideAllowlist() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate()), SIGNING_TIME,
                SignatureMethod.RSA_SHA256, "");
        setAlgorithm(doc, "Transform", Transform.XPATH);
        assertEquals(SignatureStatus.UNSUPPORTED_ALGORITHM, check(doc, VERIFIER).status());
    }

    @Test
    void requiresCoverageOfTheDirectInvoiceDataNode() throws Exception {
        Document doc = sellerInvoice();
        Element root = doc.getDocumentElement();
        var original = doc.getElementById("invoice");
        var other = doc.createElementNS(null, "Other");
        other.appendChild(original);
        root.appendChild(other);
        root.insertBefore(doc.createElementNS(null, "DLHDon"), root.getFirstChild());
        SignatureCheck check = check(doc, VERIFIER);
        assertEquals(SignatureStatus.REFERENCE_NOT_COVERING_INVOICE, check.status());
        assertFalse(check.coversInvoiceData());
    }

    @Test
    void rejectsExternalReferencesWithoutConnectingOrReading() throws Exception {
        Path external = temp.resolve("external-reference");
        Files.write(external, EXTERNAL_CONTENT);
        try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            socket.setSoTimeout(150);
            for (String uri : List.of("http://127.0.0.1:" + socket.getLocalPort() + "/invoice",
                    external.toUri().toString(), "external-reference")) {
                Document doc = invoice();
                sign(doc, "NBan", "seller", seller, List.of(seller.certificate()),
                        SIGNING_TIME, SignatureMethod.RSA_SHA256, uri);
                assertTimeoutPreemptively(Duration.ofSeconds(2),
                        () -> assertEquals(SignatureStatus.INVALID_REFERENCE, check(doc, VERIFIER).status()));
            }
            assertThrows(SocketTimeoutException.class, socket::accept);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"#missing", "#xpointer(id('invoice'))", "#"})
    void rejectsReferencesOutsideTheRegisteredIdSet(String uri) throws Exception {
        Document doc = sellerInvoice();
        ((Element) doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Reference").item(0)).setAttribute("URI", uri);
        assertEquals(SignatureStatus.INVALID_REFERENCE, check(doc, VERIFIER).status());
    }

    @Test
    void rejectsDuplicateIdsAnywhereWithoutIncludingTheValueInTheError() {
        String xml = "<HDon><DLHDon Id='sensitive-marker'/><extra><node Id='sensitive-marker'/></extra></HDon>";
        var error = assertThrows(InvoiceXmlException.class, () -> VERIFIER.verify(bytes(xml)));
        assertEquals(InvoiceXmlErrorCode.DUPLICATE_ID, error.code());
        assertFalse(error.getMessage().contains("sensitive-marker"));
        assertTrue(InvoiceXmlParser.defaults().parse(bytes(xml)).signatures().isEmpty());
    }

    @Test
    void registersOnlyUnnamespacedIdAttributes() {
        var result = VERIFIER.verify(bytes(
                "<HDon xmlns:x='urn:test'><DLHDon Id='one'/><Other x:Id='one' ID='one' id='one'/></HDon>"));
        assertTrue(result.checks().isEmpty());
    }

    @Test
    void classifiesDoctypeInEitherEncoding() {
        String xml = "<!DOCTYPE HDon SYSTEM 'file:///unavailable-invoice.dtd'><HDon/>";
        for (var encoding : List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16)) {
            var error = assertThrows(InvoiceXmlException.class, () -> VERIFIER.verify(xml.getBytes(encoding)));
            assertEquals(InvoiceXmlErrorCode.DOCTYPE_NOT_ALLOWED, error.code());
        }
    }

    @Test
    void classifiesMalformedXmlAndUnexpectedRootWithoutXmlValues() {
        var malformed = assertThrows(InvoiceXmlException.class, () -> VERIFIER.verify(bytes("<HDon>private")));
        var wrongRoot = assertThrows(InvoiceXmlException.class, () -> VERIFIER.verify(bytes("<private/>")));
        assertEquals(InvoiceXmlErrorCode.MALFORMED_XML, malformed.code());
        assertEquals(InvoiceXmlErrorCode.UNEXPECTED_ROOT, wrongRoot.code());
        assertFalse(malformed.getMessage().contains("private"));
        assertFalse(wrongRoot.getMessage().contains("private"));
    }

    @Test
    void enforcesTheConfiguredByteLimitBeforeParsing() {
        byte[] xml = bytes("<HDon/>");
        assertTrue(InvoiceSignatureVerifier.builder().maxInputBytes(xml.length).build().verify(xml).checks().isEmpty());
        var restricted = InvoiceSignatureVerifier.builder().maxInputBytes(xml.length - 1).build();
        assertEquals(InvoiceXmlErrorCode.INPUT_TOO_LARGE,
                assertThrows(InvoiceXmlException.class, () -> restricted.verify(xml)).code());
        assertThrows(IllegalArgumentException.class, () -> InvoiceSignatureVerifier.builder().maxInputBytes(0));
    }

    @Test
    void rejectsTenMiBOfElementsBeforeBuildingTheDom() {
        int byteLimit = 10 * 1024 * 1024;
        String start = "<HDon>   ";
        String end = "</HDon>";
        byte[] xml = bytes(start + "<a/>".repeat((byteLimit - start.length() - end.length()) / 4) + end);
        assertEquals(byteLimit, xml.length);

        var error = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(InvoiceXmlException.class, () -> VERIFIER.verify(xml)));
        assertEquals(InvoiceXmlErrorCode.TOO_MANY_ELEMENTS, error.code());
    }

    @Test
    void enforcesTheConfiguredElementLimitIncludingTheRoot() {
        var verifier = InvoiceSignatureVerifier.builder().maxElements(2).build();
        assertTrue(verifier.verify(bytes("<HDon xmlns:x='urn:test'><x:a/></HDon>")).checks().isEmpty());
        var error = assertThrows(InvoiceXmlException.class,
                () -> verifier.verify(bytes("<HDon xmlns:x='urn:test'><x:a><x:b/></x:a></HDon>")));
        assertEquals(InvoiceXmlErrorCode.TOO_MANY_ELEMENTS, error.code());
        assertThrows(IllegalArgumentException.class, () -> InvoiceSignatureVerifier.builder().maxElements(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NLap", "TGia"})
    void verifiesSignaturesEvenWhenBusinessFieldsCannotBeParsed(String tag) throws Exception {
        Document doc = invoice();
        Element field = doc.createElementNS(null, tag);
        field.setTextContent("invalid-business-value");
        doc.getElementsByTagName("TTChung").item(0).appendChild(field);
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        byte[] xml = serialize(doc);

        assertThrows(InvoiceXmlException.class, () -> InvoiceXmlParser.defaults().parse(xml));
        assertEquals(SignatureStatus.VALID, VERIFIER.verify(xml).checks().get(0).status());
    }

    @Test
    void reportsCoverageOfInvoiceDataAndTaxCode() throws Exception {
        Document doc = invoice();
        sign(doc, "CQT", "authority", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice", "#code");
        var check = check(doc, VERIFIER);
        assertEquals(SignerRole.TAX_AUTHORITY, check.role());
        assertEquals(SignatureStatus.VALID, check.status());
        assertTrue(check.coversInvoiceData());
        assertTrue(check.coversTaxAuthorityCode());
    }

    @Test
    void wholeDocumentEnvelopedReferenceCoversBothDirectElements() throws Exception {
        Document doc = invoice();
        sign(doc, "CQT", "authority", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA384, "");
        var check = check(doc, VERIFIER);
        assertEquals(SignatureStatus.VALID, check.status());
        assertTrue(check.coversInvoiceData());
        assertTrue(check.coversTaxAuthorityCode());
    }

    @Test
    void rejectsExpiredCertificateAtClaimedSigningTimeEvenWithTrustAnchor() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", expired, List.of(expired.certificate(), intermediate),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        var check = check(doc, withAnchor(root));
        assertEquals(SignatureStatus.CERTIFICATE_NOT_VALID_AT_SIGNING_TIME, check.status());
        assertEquals(TrustStatus.NOT_CHECKED, check.trust());
        assertTrue(check.coversInvoiceData());
    }

    @Test
    void verifiesAnEmbeddedCertificateChainAgainstOnlySuppliedAnchors() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate(), intermediate),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        assertEquals(TrustStatus.TRUSTED, check(doc, withAnchor(root)).trust());
        assertEquals(TrustStatus.UNTRUSTED, check(doc, withAnchor(unrelatedRoot)).trust());
    }

    @Test
    void suppliesOfflineIntermediateWhenTheSignatureContainsOnlyTheLeaf() throws Exception {
        Document doc = sellerInvoice();
        var certificates = new ArrayList<>(List.of(intermediate));
        var anchors = new java.util.HashSet<>(Set.of(new TrustAnchor(root, null)));
        var verifier = InvoiceSignatureVerifier.builder().trustAnchors(anchors)
                .intermediateCertificates(certificates).build();
        certificates.clear();
        anchors.clear();
        assertEquals(TrustStatus.TRUSTED, check(doc, verifier).trust());
        assertEquals(TrustStatus.UNTRUSTED, check(doc, withAnchor(root)).trust());
    }

    @Test
    void rejectsRepeatedIssuerVariantsPromptlyWithAnUnrelatedAnchor() throws Exception {
        assertEquals(10, repeatedIssuers.stream().map(X509Certificate::getSerialNumber).distinct().count());
        Document doc = invoice();
        var certificates = new ArrayList<>(List.of(seller.certificate()));
        certificates.addAll(repeatedIssuers);
        sign(doc, "NBan", "seller", seller, certificates, SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        byte[] xml = serialize(doc);
        var verifier = withAnchor(unrelatedRoot);

        var check = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> verifier.verify(xml)).checks().get(0);
        assertEquals(SignatureStatus.VALID, check.status());
        assertEquals(TrustStatus.UNTRUSTED, check.trust());
    }

    @Test
    void prefersApplicationIntermediateOverAnEmbeddedVariantWithTheSameKey() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate(), repeatedIssuers.get(0)),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        var verifier = InvoiceSignatureVerifier.builder().trustAnchors(Set.of(new TrustAnchor(root, null)))
                .intermediateCertificates(List.of(intermediate)).build();

        assertEquals(TrustStatus.UNTRUSTED, check(doc, withAnchor(root)).trust());
        assertEquals(TrustStatus.TRUSTED, check(doc, verifier).trust());
    }

    @Test
    void absentSigningTimeSkipsValidityButCannotEstablishTrust() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", expired, List.of(expired.certificate(), intermediate),
                null, SignatureMethod.RSA_SHA512, "#invoice");
        var check = check(doc, withAnchor(root));
        assertNull(check.signingTime());
        assertEquals(SignatureStatus.VALID, check.status());
        assertEquals(TrustStatus.UNTRUSTED, check.trust());
        assertEquals(TrustStatus.NOT_CHECKED, check(doc, VERIFIER).trust());
    }

    @Test
    void reportsMissingCertificateEvenWhenKeyValueIsPresent() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        assertEquals(SignatureStatus.MISSING_CERTIFICATE, check(doc, VERIFIER).status());
    }

    @Test
    void malformedSignatureDoesNotPreventCheckingOtherSignatures() throws Exception {
        Document doc = sellerInvoice();
        var malformed = doc.createElementNS(XMLSignature.XMLNS, "ds:Signature");
        doc.getElementsByTagName("NBan").item(0).appendChild(malformed);
        var checks = VERIFIER.verify(serialize(doc)).checks();
        assertEquals(List.of(SignatureStatus.VALID, SignatureStatus.MALFORMED),
                checks.stream().map(SignatureCheck::status).toList());
        assertEquals(List.of("HDon/DSCKS/NBan/Signature[1]", "HDon/DSCKS/NBan/Signature[2]"),
                checks.stream().map(SignatureCheck::path).toList());
    }

    @Test
    void pathsRolesAndOrderMatchTheParserIncludingRepeatedContainers() throws Exception {
        Document doc = invoice();
        sign(doc, "NMua", "buyer", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        sign(doc, "NBan", "seller1", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        sign(doc, "NBan", "seller2", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        sign(doc, "CQT", "authority", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        var repeated = doc.createElementNS(null, "DSCKS");
        doc.getDocumentElement().appendChild(repeated);
        repeated.appendChild(doc.getElementsByTagName("CQT").item(0));
        repeated.appendChild(doc.createElementNS(XMLSignature.XMLNS, "ds:Signature"));
        doc.getDocumentElement().appendChild(doc.createElementNS(XMLSignature.XMLNS, "ds:Signature"));
        byte[] xml = serialize(doc);

        var checks = VERIFIER.verify(xml).checks();
        var metadata = InvoiceXmlParser.defaults().parse(xml).signatures();
        assertEquals(metadata, checks.stream().map(c -> new InvoiceSignature(c.role(), c.signingTime(), c.certificate()))
                .toList());
        assertEquals(List.of("HDon/DSCKS[1]/NMua/Signature", "HDon/DSCKS[1]/NBan/Signature[1]",
                "HDon/DSCKS[1]/NBan/Signature[2]", "HDon/DSCKS[2]/CQT/Signature"),
                checks.stream().map(SignatureCheck::path).toList());
    }

    @Test
    void reportsNoSignaturesAndDefensivelyCopiesChecks() throws Exception {
        var source = new ArrayList<>(VERIFIER.verify(serialize(sellerInvoice())).checks());
        var verification = new SignatureVerification(source);
        source.clear();
        assertEquals(1, verification.checks().size());
        assertThrows(UnsupportedOperationException.class, () -> verification.checks().clear());
        assertTrue(VERIFIER.verify(bytes("<HDon/>")).checks().isEmpty());
    }

    private static Document invoice() throws Exception {
        var factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes("""
                <HDon><DLHDon Id="invoice"><TTChung><KHHDon>C24TST</KHHDon></TTChung></DLHDon>
                <MCCQT Id="code">test-code</MCCQT><DSCKS/></HDon>""")));
        ((Element) doc.getElementsByTagName("DLHDon").item(0)).setIdAttribute("Id", true);
        ((Element) doc.getElementsByTagName("MCCQT").item(0)).setIdAttribute("Id", true);
        return doc;
    }

    private static Document sellerInvoice() throws Exception {
        Document doc = invoice();
        sign(doc, "NBan", "seller", seller, List.of(seller.certificate()),
                SIGNING_TIME, SignatureMethod.RSA_SHA256, "#invoice");
        return doc;
    }

    private static void sign(Document doc, String role, String id, SigningIdentity identity,
                             List<X509Certificate> certificates, Instant time, String algorithm, String... uris)
            throws Exception {
        Element container = (Element) doc.getElementsByTagName("DSCKS").item(0);
        Element parent = (Element) container.getElementsByTagName(role).item(0);
        if (parent == null) {
            parent = doc.createElementNS(null, role);
            container.appendChild(parent);
        }

        var factory = XMLSignatureFactory.getInstance("DOM");
        var context = new DOMSignContext(identity.key(), parent);
        context.setDefaultNamespacePrefix("ds");
        var jdkResolver = factory.getURIDereferencer();
        context.setURIDereferencer((reference, cryptoContext) ->
                reference.getURI().isEmpty() || reference.getURI().startsWith("#")
                        ? jdkResolver.dereference(reference, cryptoContext)
                        : new OctetStreamData(new ByteArrayInputStream(EXTERNAL_CONTENT)));
        var digest = factory.newDigestMethod(DigestMethod.SHA256, null);
        var references = new ArrayList<Reference>();
        for (String uri : uris) {
            var transforms = uri.isEmpty()
                    ? List.of(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null))
                    : List.<Transform>of();
            references.add(factory.newReference(uri, digest, transforms, null, null));
        }

        var objects = new ArrayList<javax.xml.crypto.dsig.XMLObject>();
        if (time != null) {
            Element qualifying = doc.createElementNS(XADES, "xades:QualifyingProperties");
            qualifying.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", XADES);
            qualifying.setAttribute("Target", "#" + id);
            Element properties = doc.createElementNS(XADES, "xades:SignedProperties");
            properties.setAttribute("Id", id + "-properties");
            context.setIdAttributeNS(properties, null, "Id");
            qualifying.appendChild(properties);
            Element signatureProperties = doc.createElementNS(XADES, "xades:SignedSignatureProperties");
            properties.appendChild(signatureProperties);
            Element signingTime = doc.createElementNS(XADES, "xades:SigningTime");
            signingTime.setTextContent(time.toString());
            signatureProperties.appendChild(signingTime);
            objects.add(factory.newXMLObject(List.of(new DOMStructure(qualifying)), null, null, null));
            references.add(factory.newReference("#" + id + "-properties", digest, List.of(), SIGNED_PROPERTIES, null));
        }

        var methods = factory.getKeyInfoFactory();
        var keyInfo = methods.newKeyInfo(certificates.isEmpty()
                ? List.of(methods.newKeyValue(identity.certificate().getPublicKey()))
                : List.of(methods.newX509Data(certificates)));
        var signedInfo = factory.newSignedInfo(
                factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                factory.newSignatureMethod(algorithm, null), references);
        factory.newXMLSignature(signedInfo, keyInfo, objects, id, null).sign(context);
    }

    private static void setAlgorithm(Document doc, String method, String algorithm) {
        ((Element) doc.getElementsByTagNameNS(XMLSignature.XMLNS, method).item(0)).setAttribute("Algorithm", algorithm);
    }

    private static SignatureCheck check(Document doc, InvoiceSignatureVerifier verifier) throws Exception {
        return verifier.verify(serialize(doc)).checks().get(0);
    }

    private static InvoiceSignatureVerifier withAnchor(X509Certificate anchor) {
        return InvoiceSignatureVerifier.builder().trustAnchors(Set.of(new TrustAnchor(anchor, null))).build();
    }

    private static byte[] serialize(Document doc) throws Exception {
        var output = new ByteArrayOutputStream();
        TransformerFactory.newDefaultInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(output));
        return output.toByteArray();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void generateKey(String alias, String start, int days, boolean ca) throws Exception {
        keytool("-genkeypair", "-alias", alias, "-dname", "CN=Synthetic " + alias, "-keyalg", "RSA",
                "-keysize", "2048", "-keystore", storePath(alias).toString(),
                "-startdate", start, "-validity", Integer.toString(days), "-ext", "bc=ca:" + ca);
    }

    private static X509Certificate issue(String subject, String issuer, String start, int days, boolean ca)
            throws Exception {
        return issue(certificateRequest(subject), issuer, subject, start, days, ca);
    }

    private static Path certificateRequest(String subject) throws Exception {
        Path request = temp.resolve(subject + ".csr");
        keytool("-certreq", "-alias", subject, "-keystore", storePath(subject).toString(), "-file", request.toString());
        return request;
    }

    private static X509Certificate issue(Path request, String issuer, String name, String start, int days, boolean ca)
            throws Exception {
        Path output = temp.resolve(name + ".cer");
        keytool("-gencert", "-alias", issuer, "-keystore", storePath(issuer).toString(), "-infile", request.toString(),
                "-outfile", output.toString(), "-startdate", start, "-validity", Integer.toString(days),
                "-ext", "bc=ca:" + ca);
        try (var stream = Files.newInputStream(output)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream);
        }
    }

    private static void keytool(String... args) throws Exception {
        String binary = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", binary).toString()));
        command.addAll(Arrays.asList(args));
        command.addAll(List.of("-storetype", "PKCS12", "-storepass", STORE_PASSWORD));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Keytool timed out");
        }
        assertEquals(0, process.exitValue(), () -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException ex) {
                return "Could not read keytool diagnostics";
            }
        });
    }

    private static Path storePath(String alias) {
        return temp.resolve(alias + ".p12");
    }

    private static KeyStore keyStore(String alias) throws Exception {
        return KeyStore.getInstance(storePath(alias).toFile(), STORE_PASSWORD.toCharArray());
    }

    private static SigningIdentity identity(String alias, X509Certificate certificate) throws Exception {
        return new SigningIdentity((PrivateKey) keyStore(alias).getKey(alias, STORE_PASSWORD.toCharArray()), certificate);
    }

    private record SigningIdentity(PrivateKey key, X509Certificate certificate) {
    }
}
