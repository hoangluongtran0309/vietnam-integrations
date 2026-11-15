package io.github.hoangluongtran0309.vietnam.zalo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** ZBS Template Message sent to a normalized Vietnamese phone number. */
public record ZbsPhoneTemplateRequest(
        String phone, String templateId, Map<String, Object> templateData, String trackingId) {

    private static final Pattern PHONE_CHARACTERS = Pattern.compile("[+0-9 .()\\-]+");
    private static final Pattern NATIONAL_PHONE = Pattern.compile("0\\d{9}");
    private static final Pattern COUNTRY_CODE_PHONE = Pattern.compile("(?:\\+?84)\\d{9}");
    private static final Pattern TRACKING = Pattern.compile("[A-Za-z0-9_-]{1,48}");

    public ZbsPhoneTemplateRequest {
        phone = normalizePhone(phone);
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        Objects.requireNonNull(templateData, "templateData");
        templateData.forEach(ZbsPhoneTemplateRequest::validateTemplateDataEntry);
        templateData = Map.copyOf(templateData);
        if (trackingId == null || !TRACKING.matcher(trackingId).matches()) {
            throw new IllegalArgumentException("trackingId must contain 1-48 letters, digits, '_' or '-'");
        }
    }

    @Override
    public String toString() {
        return "ZbsPhoneTemplateRequest[phone=" + ZaloRedaction.recipient(phone)
                + ", templateId=" + templateId + ", templateData=" + templateData.keySet()
                + ", trackingId=" + trackingId + "]";
    }

    private static String normalizePhone(String phone) {
        if (phone == null || !PHONE_CHARACTERS.matcher(phone).matches()) {
            throw invalidPhone();
        }
        String compact = phone.replaceAll("[ .()\\-]", "");
        // TODO(v0.2): verify the exact ZBS phone format — capture in WP-2D.1.
        if (NATIONAL_PHONE.matcher(compact).matches()) {
            return "84" + compact.substring(1);
        }
        if (COUNTRY_CODE_PHONE.matcher(compact).matches()) {
            return compact.charAt(0) == '+' ? compact.substring(1) : compact;
        }
        throw invalidPhone();
    }

    private static IllegalArgumentException invalidPhone() {
        return new IllegalArgumentException(
                "phone must contain 0 followed by 9 digits, or country code 84 followed by 9 digits");
    }

    private static void validateTemplateDataEntry(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("templateData key must not be blank");
        }
        // TODO(v0.2): verify whether ZBS requires every template parameter to be a string.
        if (!(value instanceof String || value instanceof Boolean
                || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte
                || value instanceof BigInteger || value instanceof BigDecimal)) {
            throw new IllegalArgumentException("templateData value for key '" + key
                    + "' must be a string, boolean, integer, BigInteger or BigDecimal");
        }
    }
}
