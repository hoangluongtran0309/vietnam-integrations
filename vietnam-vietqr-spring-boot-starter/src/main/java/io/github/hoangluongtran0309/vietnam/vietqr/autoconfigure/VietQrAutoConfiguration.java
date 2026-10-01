package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
    VietQrGenerator vietQrGenerator(VietQrEncoder encoder, VietQrProperties properties) {
        return new VietQrGenerator(encoder, properties);
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
