package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableLookup;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PaymentReconciler;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnClass(VietQrEncoder.class)
@ConditionalOnProperty(prefix = "vietnam.vietqr", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(VietQrProperties.class)
public class VietQrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    VietQrEncoder vietQrEncoder() {
        return new VietQrEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    VietQrDecoder vietQrDecoder() {
        return new VietQrDecoder();
    }

    @Bean
    @ConditionalOnMissingBean
    VietQrGenerator vietQrGenerator(
            VietQrEncoder encoder,
            VietQrProperties properties,
            ObjectProvider<PaymentReferenceFormat> referenceFormats) {
        return new VietQrGenerator(encoder, properties, referenceFormats.getIfUnique());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "vietnam.vietqr.reference", name = "prefix")
    PaymentReferenceFormat paymentReferenceFormat(VietQrProperties properties) {
        try {
            return PaymentReferenceFormat.of(properties.getReference().getPrefix());
        } catch (VietQrException | NullPointerException e) {
            throw new VietQrException("Invalid vietnam.vietqr.reference.prefix: " + e.getMessage());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({PaymentReferenceFormat.class, PayableLookup.class})
    PaymentReconciler paymentReconciler(PaymentReferenceFormat format, PayableLookup lookup) {
        return new PaymentReconciler(format, lookup);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.google.zxing.client.j2se.MatrixToImageWriter")
    static class ImageRendererConfiguration {

        @Bean
        @ConditionalOnMissingBean
        QrImageRenderer qrImageRenderer() {
            return new QrImageRenderer();
        }
    }
}
