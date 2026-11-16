package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookRouter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;

@AutoConfiguration(after = ZaloOaAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "webhook.enabled", havingValue = "true")
public class ZaloWebhookAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ZaloWebhookController zaloWebhookController(
            ZaloWebhookRouter router, ApplicationEventPublisher publisher) {
        return new ZaloWebhookController(router, publisher);
    }
}
