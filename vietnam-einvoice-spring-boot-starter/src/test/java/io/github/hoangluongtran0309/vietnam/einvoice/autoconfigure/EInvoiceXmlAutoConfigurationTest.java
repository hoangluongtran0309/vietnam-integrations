package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceService;
import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspector;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.InvoiceSignatureVerifier;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.TrustStatus;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceRule;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlErrorCode;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlException;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;
import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.w3c.dom.Element;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class EInvoiceXmlAutoConfigurationTest {

    private static final byte[] DATED_INVOICE = bytes("""
            <HDon><DLHDon><TTChung><NLap>2000-01-02</NLap></TTChung></DLHDon></HDon>
            """);
    private static final Clock EARLY_CLOCK = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String TEST_STORE_PASSWORD = "synthetic-test";

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(EInvoiceAutoConfiguration.class, EInvoiceXmlAutoConfiguration.class));

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsInvoiceReadingBeansWithoutAnIssuingProvider() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(InvoiceXmlParser.class)
                .hasSingleBean(InvoiceSignatureVerifier.class)
                .hasSingleBean(InvoiceValidator.class)
                .hasSingleBean(InvoiceInspector.class)
                .doesNotHaveBean(EInvoiceService.class));
    }

    @Test
    void applicationBeansOverrideAllDefaults() {
        var parser = InvoiceXmlParser.defaults();
        var verifier = InvoiceSignatureVerifier.defaults();
        var validator = InvoiceValidator.defaults();
        var inspector = InvoiceInspector.defaults();

        runner.withBean(InvoiceXmlParser.class, () -> parser)
                .withBean(InvoiceSignatureVerifier.class, () -> verifier)
                .withBean(InvoiceValidator.class, () -> validator)
                .withBean(InvoiceInspector.class, () -> inspector)
                .run(context -> {
                    assertThat(context).hasSingleBean(InvoiceXmlParser.class).hasSingleBean(InvoiceSignatureVerifier.class)
                            .hasSingleBean(InvoiceValidator.class).hasSingleBean(InvoiceInspector.class);
                    assertThat(context.getBean(InvoiceXmlParser.class)).isSameAs(parser);
                    assertThat(context.getBean(InvoiceSignatureVerifier.class)).isSameAs(verifier);
                    assertThat(context.getBean(InvoiceValidator.class)).isSameAs(validator);
                    assertThat(context.getBean(InvoiceInspector.class)).isSameAs(inspector);
                });
    }

    @ParameterizedTest
    @CsvSource({
            "max-elements=1, TOO_MANY_ELEMENTS",
            "max-input-size=8B, INPUT_TOO_LARGE"
    })
    void appliesXmlLimitsToParserAndVerifier(String property, InvoiceXmlErrorCode code) {
        runner.withPropertyValues("vietnam.einvoice.xml." + property).run(context -> {
            var inspector = context.getBean(InvoiceInspector.class);
            var verifier = context.getBean(InvoiceSignatureVerifier.class);
            assertThatExceptionOfType(InvoiceXmlException.class)
                    .isThrownBy(() -> inspector.inspect(DATED_INVOICE))
                    .satisfies(error -> assertThat(error.code()).isEqualTo(code));
            assertThatExceptionOfType(InvoiceXmlException.class)
                    .isThrownBy(() -> verifier.verify(DATED_INVOICE))
                    .satisfies(error -> assertThat(error.code()).isEqualTo(code));
        });
    }

    @Test
    void appliesAmountTolerance() {
        byte[] xml = bytes("""
                <HDon><DLHDon><NDHDon><DSHHDVu><HHDVu><TChat>1</TChat>
                <SLuong>2</SLuong><DGia>2</DGia><ThTien>4.50</ThTien>
                </HHDVu></DSHHDVu></NDHDon></DLHDon></HDon>
                """);

        runner.run(context -> assertThat(context.getBean(InvoiceInspector.class).inspect(xml).report()
                .withCode("EINV-AMT-001")).extracting(ValidationIssue::severity).containsExactly(Severity.INFO));
        runner.withPropertyValues("vietnam.einvoice.validation.amount-tolerance=0")
                .run(context -> assertThat(context.getBean(InvoiceInspector.class).inspect(xml).report()
                        .withCode("EINV-AMT-001")).extracting(ValidationIssue::severity).containsExactly(Severity.ERROR));
    }

    @Test
    void rejectsNegativeAmountTolerance() {
        runner.withPropertyValues("vietnam.einvoice.validation.amount-tolerance=-1").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "vietnam.einvoice.validation.amount-tolerance must be non-negative");
        });
    }

    @Test
    void disablesSignatureVerificationOnlyForTheInspector() {
        byte[] xml = bytes("""
                <HDon xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                <DSCKS><NBan><ds:Signature/></NBan></DSCKS>
                </HDon>
                """);

        runner.withPropertyValues("vietnam.einvoice.signature.enabled=false").run(context -> {
            var result = context.getBean(InvoiceInspector.class).inspect(xml);
            assertThat(result.document().signatures()).hasSize(1);
            assertThat(result.signatures().checks()).isEmpty();
            assertThat(result.report().withCode("EINV-SIG-002")).isEmpty();
            assertThat(context.getBean(InvoiceSignatureVerifier.class).verify(xml).checks()).hasSize(1);
        });
    }

    @Test
    void runsApplicationRulesInSpringOrder() {
        runner.withUserConfiguration(OrderedRules.class).run(context -> {
            var report = context.getBean(InvoiceInspector.class).inspect(DATED_INVOICE).report();
            assertThat(report.issues()).extracting(ValidationIssue::code)
                    .filteredOn(code -> code.startsWith("APP-")).containsExactly("APP-FIRST", "APP-SECOND");
        });
    }

    @Test
    void usesTheApplicationsUniqueClock() {
        runner.withBean(Clock.class, () -> EARLY_CLOCK).run(context -> {
            var report = context.getBean(InvoiceInspector.class).inspect(DATED_INVOICE).report();
            assertThat(report.withCode("EINV-DATE-001")).hasSize(1);
        });
    }

    @Test
    void usesDefaultClockWhenNoApplicationClockIsUnique() {
        runner.withBean("earlyClock", Clock.class, () -> EARLY_CLOCK)
                .withBean("otherClock", Clock.class, () -> EARLY_CLOCK)
                .run(context -> assertThat(context.getBean(InvoiceInspector.class).inspect(DATED_INVOICE).report()
                        .withCode("EINV-DATE-001")).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"trust-anchors, true", "intermediate-certificates, false"})
    void rejectsInvalidOrMissingCertificateWithPropertyAndIndex(String property, boolean createFile) throws Exception {
        String invalidContent = "synthetic invalid certificate contents";
        Path path = temporaryDirectory.resolve("invalid");
        if (createFile) {
            Files.writeString(path, invalidContent);
        }

        String indexedProperty = "vietnam.einvoice.signature." + property + "[0]";
        runner.withPropertyValues(indexedProperty + "=" + path.toUri()).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(indexedProperty + " must reference a readable X.509 certificate");
            assertThat(Throwables.getStackTrace(context.getStartupFailure())).doesNotContain(invalidContent);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void loadsPemAndDerTrustAnchors(boolean pem) throws Exception {
        Path store = temporaryDirectory.resolve("signer.p12");
        Path certificateFile = temporaryDirectory.resolve("certificate");
        keytool("-genkeypair", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=Synthetic Test Anchor",
                "-validity", "2", "-keystore", store.toString(), "-alias", "signer");
        if (pem) {
            keytool("-exportcert", "-rfc", "-keystore", store.toString(), "-alias", "signer",
                    "-file", certificateFile.toString());
        } else {
            keytool("-exportcert", "-keystore", store.toString(), "-alias", "signer",
                    "-file", certificateFile.toString());
        }
        byte[] xml = signInvoice(store);

        runner.withPropertyValues(
                        "vietnam.einvoice.signature.trust-anchors[0]=" + certificateFile.toUri(),
                        "vietnam.einvoice.signature.intermediate-certificates[0]=" + certificateFile.toUri())
                .run(context -> assertThat(context.getBean(InvoiceSignatureVerifier.class).verify(xml).checks())
                        .singleElement().satisfies(check -> assertThat(check.trust()).isEqualTo(TrustStatus.TRUSTED)));
    }

    private static byte[] signInvoice(Path storePath) throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        char[] password = TEST_STORE_PASSWORD.toCharArray();
        try (var input = Files.newInputStream(storePath)) {
            store.load(input, password);
        }
        var certificate = (X509Certificate) store.getCertificate("signer");
        var key = (PrivateKey) store.getKey("signer", password);
        var documents = DocumentBuilderFactory.newDefaultInstance();
        documents.setNamespaceAware(true);
        var xml = documents.newDocumentBuilder().parse(new ByteArrayInputStream(bytes(
                "<HDon><DLHDon Id=\"invoice\"/><DSCKS><NBan/></DSCKS></HDon>")));
        var invoice = (Element) xml.getElementsByTagName("DLHDon").item(0);
        var context = new DOMSignContext(key, xml.getElementsByTagName("NBan").item(0));
        context.setIdAttributeNS(invoice, null, "Id");
        context.setDefaultNamespacePrefix("ds");

        var factory = XMLSignatureFactory.getInstance("DOM");
        var reference = factory.newReference("#invoice", factory.newDigestMethod(DigestMethod.SHA256, null));
        var canonicalization = factory.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
        var signedInfo = factory.newSignedInfo(canonicalization,
                factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null), List.of(reference));
        var keys = factory.getKeyInfoFactory();
        var keyInfo = keys.newKeyInfo(List.of(keys.newX509Data(List.of(certificate))));
        var signingTime = xml.createElementNS(null, "SigningTime");
        signingTime.setTextContent(certificate.getNotBefore().toInstant().plusSeconds(1).toString());
        var object = factory.newXMLObject(List.of(new DOMStructure(signingTime)), null, null, null);
        factory.newXMLSignature(signedInfo, keyInfo, List.of(object), null, null).sign(context);

        var output = new ByteArrayOutputStream();
        TransformerFactory.newDefaultInstance().newTransformer().transform(new DOMSource(xml), new StreamResult(output));
        return output.toByteArray();
    }

    private static void keytool(String... arguments) throws Exception {
        String binary = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        var command = new ArrayList<>(
                List.of(Path.of(System.getProperty("java.home"), "bin", binary).toString()));
        command.addAll(Arrays.asList(arguments));
        command.addAll(List.of("-storetype", "PKCS12", "-storepass", TEST_STORE_PASSWORD));
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Keytool timed out");
        }
        assertThat(process.exitValue()).as(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isZero();
    }

    private static byte[] bytes(String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedRules {

        @Bean
        @Order(20)
        InvoiceRule secondRule() {
            return document -> List.of(new ValidationIssue("APP-SECOND", Severity.INFO, "HDon", "Second check."));
        }

        @Bean
        @Order(10)
        InvoiceRule firstRule() {
            return document -> List.of(new ValidationIssue("APP-FIRST", Severity.INFO, "HDon", "First check."));
        }
    }
}
