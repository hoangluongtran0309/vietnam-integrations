package io.github.hoangluongtran0309.vietnam.zalo;

/** Port for ZBS Template Messages. Version 0.2 supports delivery by phone number. */
public interface ZaloZbsClient {
    ZaloMessageResult sendTemplateByPhone(ZbsPhoneTemplateRequest request);
}
