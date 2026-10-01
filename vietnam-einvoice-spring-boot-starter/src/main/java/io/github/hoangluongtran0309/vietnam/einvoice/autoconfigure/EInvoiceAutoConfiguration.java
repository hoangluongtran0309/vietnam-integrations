package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceProvider;
import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "vietnam.einvoice", name = "provider")
@EnableConfigurationProperties(EInvoiceProperties.class)
public class EInvoiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EInvoiceService eInvoiceService(ObjectProvider<EInvoiceProvider> providers, EInvoiceProperties properties) {
        // Provider adapters (vietnam-einvoice-provider-*) register their own EInvoiceProvider beans.
        return new EInvoiceService(providers.orderedStream().toList(), properties.getProvider());
    }
}
