package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloConsultationTextRequest;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloMessageResult;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOa;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZbsPhoneTemplateRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", havingValue = "true")
class ZaloMessagingController {

    private final ZaloOaRegistry registry;

    ZaloMessagingController(ZaloOaRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/zalo/consultation")
    ZaloMessageResult consultation(@RequestParam(required = false) String account,
                                   @RequestParam String userId, @RequestParam String text) {
        return selectAccount(account).messaging()
                .sendConsultationText(new ZaloConsultationTextRequest(userId, text));
    }

    @PostMapping("/zalo/zbs")
    ZaloMessageResult zbs(@RequestParam(required = false) String account, @RequestBody ZbsDemoRequest request) {
        return selectAccount(account).zbs().sendTemplateByPhone(new ZbsPhoneTemplateRequest(
                request.phone(), request.templateId(), request.templateData(), request.trackingId()));
    }

    private ZaloOa selectAccount(String account) {
        if (account == null || account.isBlank()) {
            return registry.defaultOa().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "account is required when no vietnam.zalo-oa.default-account is configured"));
        }
        try {
            return registry.oa(account);
        } catch (ZaloException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    record ZbsDemoRequest(String phone, String templateId, Map<String, Object> templateData, String trackingId) {
    }
}
