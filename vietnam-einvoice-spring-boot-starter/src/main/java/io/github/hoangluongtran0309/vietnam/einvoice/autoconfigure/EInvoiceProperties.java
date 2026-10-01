package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vietnam.einvoice")
public class EInvoiceProperties {

    /** Id of the provider used by EInvoiceService#issue, e.g. "viettel". */
    private String provider;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
