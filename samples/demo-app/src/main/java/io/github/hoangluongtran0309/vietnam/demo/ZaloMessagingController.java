package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloConsultationTextRequest;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloMessageResult;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloZbsClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZbsPhoneTemplateRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnBean({ZaloOaClient.class, ZaloZbsClient.class})
class ZaloMessagingController {

    private final ZaloOaClient oaClient;
    private final ZaloZbsClient zbsClient;

    ZaloMessagingController(ZaloOaClient oaClient, ZaloZbsClient zbsClient) {
        this.oaClient = oaClient;
        this.zbsClient = zbsClient;
    }

    @PostMapping("/zalo/consultation")
    ZaloMessageResult consultation(@RequestParam String userId, @RequestParam String text) {
        return oaClient.sendConsultationText(new ZaloConsultationTextRequest(userId, text));
    }

    @PostMapping("/zalo/zbs")
    ZaloMessageResult zbs(@RequestBody ZbsDemoRequest request) {
        return zbsClient.sendTemplateByPhone(new ZbsPhoneTemplateRequest(
                request.phone(), request.templateId(), request.templateData(), request.trackingId()));
    }

    record ZbsDemoRequest(String phone, String templateId, Map<String, Object> templateData, String trackingId) {
    }
}
