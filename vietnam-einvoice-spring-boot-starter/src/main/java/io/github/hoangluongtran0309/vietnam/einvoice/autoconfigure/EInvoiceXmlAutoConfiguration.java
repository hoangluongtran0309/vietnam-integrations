package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspector;
import io.github.hoangluongtran0309.vietnam.einvoice.signature.InvoiceSignatureVerifier;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceRule;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@EnableConfigurationProperties(EInvoiceProperties.class)
public class EInvoiceXmlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    InvoiceXmlParser invoiceXmlParser(EInvoiceProperties properties) {
        var xml = properties.getXml();
        validateLimits(xml);
        return InvoiceXmlParser.builder()
                .maxInputBytes(xml.getMaxInputSize().toBytes())
                .maxElements(xml.getMaxElements())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    InvoiceSignatureVerifier invoiceSignatureVerifier(EInvoiceProperties properties) {
        var xml = properties.getXml();
        validateLimits(xml);
        var signature = properties.getSignature();
        var anchors = certificates(signature.getTrustAnchors(), "vietnam.einvoice.signature.trust-anchors")
                .stream().map(certificate -> new TrustAnchor(certificate, null)).collect(Collectors.toSet());
        var intermediates = certificates(signature.getIntermediateCertificates(),
                "vietnam.einvoice.signature.intermediate-certificates");
        return InvoiceSignatureVerifier.builder()
                .maxInputBytes(xml.getMaxInputSize().toBytes())
                .maxElements(xml.getMaxElements())
                .trustAnchors(anchors)
                .intermediateCertificates(intermediates)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    InvoiceValidator invoiceValidator(EInvoiceProperties properties, ObjectProvider<InvoiceRule> rules,
                                      ObjectProvider<Clock> clocks) {
        var tolerance = properties.getValidation().getAmountTolerance();
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("vietnam.einvoice.validation.amount-tolerance must be non-negative");
        }
        var builder = InvoiceValidator.builder().amountTolerance(tolerance);
        Clock clock = clocks.getIfUnique();
        if (clock != null) {
            builder.clock(clock);
        }
        rules.orderedStream().forEach(builder::addRule);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    InvoiceInspector invoiceInspector(InvoiceXmlParser parser, InvoiceSignatureVerifier verifier,
                                      InvoiceValidator validator, EInvoiceProperties properties) {
        return InvoiceInspector.builder()
                .parser(parser)
                .signatureVerifier(verifier)
                .validator(validator)
                .verifySignatures(properties.getSignature().isEnabled())
                .build();
    }

    private static void validateLimits(EInvoiceProperties.Xml xml) {
        if (xml.getMaxInputSize().toBytes() <= 0) {
            throw new IllegalArgumentException("vietnam.einvoice.xml.max-input-size must be positive");
        }
        if (xml.getMaxElements() <= 0) {
            throw new IllegalArgumentException("vietnam.einvoice.xml.max-elements must be positive");
        }
    }

    private static List<X509Certificate> certificates(List<Resource> resources, String property) {
        var results = new ArrayList<X509Certificate>(resources.size());
        for (int index = 0; index < resources.size(); index++) {
            try (var input = new BufferedInputStream(resources.get(index).getInputStream())) {
                var factory = CertificateFactory.getInstance("X.509");
                results.add((X509Certificate) factory.generateCertificate(input));
            } catch (IOException | CertificateException e) {
                throw new IllegalArgumentException(property + "[" + index + "] must reference a readable X.509 certificate");
            }
        }
        return results;
    }
}
