package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Map;

/**
 * Port for OA messaging and ZNS.
 * TODO(v0.2): implement; decide on JSON library strategy (see docs/adr/0002-core-modules-framework-agnostic.md).
 */
public interface ZaloOaClient {

    /** Customer-service text message to a user who follows the OA. Returns the Zalo message id. */
    String sendText(String userId, String text);

    /** ZNS template message to a phone number (format 84xxxxxxxxx). Returns the Zalo message id. */
    String sendZns(String phone, String templateId, Map<String, String> templateData, String trackingId);
}
