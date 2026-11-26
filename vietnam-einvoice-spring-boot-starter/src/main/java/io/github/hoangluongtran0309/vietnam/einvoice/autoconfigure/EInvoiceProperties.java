package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "vietnam.einvoice")
public class EInvoiceProperties {

    /** Id of the provider used by EInvoiceService#issue, e.g. "viettel". */
    private String provider;

    /** Input limits for the XML parser and signature verifier. */
    private final Xml xml = new Xml();

    /** Validation policy for the invoice document. */
    private final Validation validation = new Validation();

    /** Signature verification and offline certificate trust settings. */
    private final Signature signature = new Signature();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Xml getXml() { return xml; }
    public Validation getValidation() { return validation; }
    public Signature getSignature() { return signature; }

    public static class Xml {

        /** Maximum XML input size, including any byte order mark. */
        private DataSize maxInputSize = DataSize.ofMegabytes(10);

        /** Maximum number of XML elements, including the root element. */
        private int maxElements = 100_000;

        public DataSize getMaxInputSize() { return maxInputSize; }
        public void setMaxInputSize(DataSize maxInputSize) { this.maxInputSize = maxInputSize; }

        public int getMaxElements() { return maxElements; }
        public void setMaxElements(int maxElements) { this.maxElements = maxElements; }
    }

    public static class Validation {

        /** Maximum absolute amount difference treated as informational, in the invoice currency. Defaults to 1. */
        private BigDecimal amountTolerance = new BigDecimal("1");

        public BigDecimal getAmountTolerance() { return amountTolerance; }
        public void setAmountTolerance(BigDecimal amountTolerance) { this.amountTolerance = amountTolerance; }
    }

    public static class Signature {

        /** Whether the invoice inspector verifies XML signatures and includes signature issues. */
        private boolean enabled = true;

        /** Trust anchors for offline PKIX validation, one PEM or DER X.509 certificate per resource. */
        private List<Resource> trustAnchors = List.of();

        /** Additional issuers for offline PKIX validation, one PEM or DER X.509 certificate per resource. */
        private List<Resource> intermediateCertificates = List.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<Resource> getTrustAnchors() { return trustAnchors; }
        public void setTrustAnchors(List<Resource> trustAnchors) { this.trustAnchors = trustAnchors; }

        public List<Resource> getIntermediateCertificates() { return intermediateCertificates; }
        public void setIntermediateCertificates(List<Resource> intermediateCertificates) {
            this.intermediateCertificates = intermediateCertificates;
        }
    }
}
