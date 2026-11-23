package io.github.hoangluongtran0309.vietnam.demo.capture;

import io.github.hoangluongtran0309.vietnam.zalo.autoconfigure.ZaloOaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Conditional(ZaloCaptureConfiguration.CaptureEnabledCondition.class)
class ZaloCaptureConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    CaptureSession zaloCaptureSession(ZaloOaProperties properties, ObjectMapper mapper, Clock clock,
                                      org.springframework.core.env.Environment environment) {
        return new CaptureSession(environment.getProperty("demo.zalo.capture-dir"), mapper, clock,
                captureAccounts(properties));
    }

    @Bean(name = "zaloHttpClient", defaultCandidate = false)
    @ConditionalOnMissingBean(name = "zaloHttpClient")
    HttpClient capturingZaloHttpClient(ZaloOaProperties properties, CaptureSession session) {
        HttpClient delegate = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        return new CapturingHttpClient(delegate, session);
    }

    private static List<CaptureSession.Account> captureAccounts(ZaloOaProperties properties) {
        List<CaptureSession.Account> accounts = new ArrayList<>();
        if (properties.getAccounts() == null || properties.getAccounts().isEmpty()) {
            accounts.add(new CaptureSession.Account("default", properties.getOaId(), properties.getAppId()));
        } else {
            properties.getAccounts().forEach((alias, account) -> accounts.add(new CaptureSession.Account(alias,
                    account.getOaId(), fallback(account.getAppId(), properties.getAppId()))));
        }
        return List.copyOf(accounts);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    static final class CaptureEnabledCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String directory = context.getEnvironment().getProperty("demo.zalo.capture-dir");
            boolean zaloEnabled = context.getEnvironment().getProperty("vietnam.zalo-oa.enabled",
                    Boolean.class, true);
            return zaloEnabled && directory != null && !directory.isBlank();
        }
    }
}
