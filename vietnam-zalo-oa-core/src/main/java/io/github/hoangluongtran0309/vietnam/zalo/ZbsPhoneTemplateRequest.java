package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** ZBS Template Message sent to a normalized Vietnamese phone number. */
public record ZbsPhoneTemplateRequest(
        String phone, String templateId, Map<String, Object> templateData, String trackingId) {

    private static final Pattern PHONE = Pattern.compile("\\+?84\\d{8,10}");
    private static final Pattern TRACKING = Pattern.compile("[A-Za-z0-9_-]{1,48}");

    public ZbsPhoneTemplateRequest {
        if (phone == null || !PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("phone must use normalized Vietnamese format 84xxxxxxxxx or +84xxxxxxxxx");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        Objects.requireNonNull(templateData, "templateData");
        templateData = Map.copyOf(templateData);
        if (trackingId == null || !TRACKING.matcher(trackingId).matches()) {
            throw new IllegalArgumentException("trackingId must contain 1-48 letters, digits, '_' or '-'");
        }
    }
}
