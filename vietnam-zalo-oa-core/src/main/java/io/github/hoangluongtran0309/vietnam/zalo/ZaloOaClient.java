package io.github.hoangluongtran0309.vietnam.zalo;

/**
 * Port for Zalo OA messaging.
 */
public interface ZaloOaClient {

    /** Sends a customer-service text message through OA API v3. */
    ZaloMessageResult sendConsultationText(ZaloConsultationTextRequest request);
}
