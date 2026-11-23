package io.github.hoangluongtran0309.vietnam.demo.capture;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class CaptureRedactor {

    private static final Set<String> SECRET = Set.of("access_token", "refresh_token", "code", "code_verifier",
            "code_challenge", "secret_key", "oa_secret_key", "app_secret");
    private static final Set<String> PSEUDONYM = Set.of("user_id", "user_id_by_app", "oa_id", "app_id", "phone",
            "msg_id", "message_id", "tracking_id", "template_id");
    private static final Set<String> ID_PARENTS = Set.of("sender", "recipient", "user", "follower");
    private static final Set<String> KEEP = Set.of("error", "event_name", "timestamp", "sent_time", "expires_in",
            "grant_type", "status", "quota");
    private static final Set<String> REMOVED_HEADERS = Set.of("secret_key", "access_token", "authorization",
            "cookie", "set-cookie");

    private final ObjectMapper mapper;
    private final byte[] key = new byte[32];
    private final Set<String> defaultFields = java.util.Collections.synchronizedSet(new TreeSet<>());

    CaptureRedactor(ObjectMapper mapper) {
        this.mapper = mapper;
        new SecureRandom().nextBytes(key);
    }

    JsonNode redactJson(byte[] body) {
        return redactNode(mapper.readTree(body), null, null);
    }

    JsonNode redactJson(String body) {
        return redactNode(mapper.readTree(body), null, null);
    }

    String redactForm(String body) {
        return redactParameters(body);
    }

    String redactQuery(String query) {
        return query == null ? null : redactParameters(query);
    }

    Map<String, List<String>> redactHeaders(Map<String, List<String>> headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (REMOVED_HEADERS.contains(normalized)) {
                return;
            }
            if (normalized.equals("x-zevent-signature")) {
                List<String> redacted = values.stream().map(CaptureRedactor::redactSignature).toList();
                result.put(name, redacted);
            } else {
                result.put(name, List.copyOf(values));
            }
        });
        return result;
    }

    Set<String> defaultFields() {
        synchronized (defaultFields) {
            return Set.copyOf(defaultFields);
        }
    }

    String pseudonymize(String original) {
        byte[] digest;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            digest = mac.doFinal(original.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
        StringBuilder result = new StringBuilder(original.length());
        for (int i = 0; i < original.length(); i++) {
            char value = original.charAt(i);
            int random = Byte.toUnsignedInt(digest[i % digest.length]);
            if (Character.isDigit(value)) {
                int floor = firstDigitIndex(original) == i ? 1 : 0;
                result.append((char) ('0' + floor + random % (10 - floor)));
            } else if (Character.isLowerCase(value)) {
                result.append((char) ('a' + random % 26));
            } else if (Character.isUpperCase(value)) {
                result.append((char) ('A' + random % 26));
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }

    private JsonNode redactNode(JsonNode node, String fieldName, String parentName) {
        if (node == null || node.isNull() || node.isBoolean()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            if (fieldName != null && fieldName.equalsIgnoreCase("template_data")) {
                node.properties().forEach(entry -> result.put(entry.getKey(), redactTemplateValue(entry.getValue())));
            } else {
                node.properties().forEach(entry -> result.set(entry.getKey(),
                        redactNode(entry.getValue(), entry.getKey(), fieldName)));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> result.add(redactNode(item, fieldName, parentName)));
            return result;
        }

        String field = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        if (SECRET.contains(field)) {
            String original = scalarText(node);
            return JsonNodeFactory.instance.textNode("REDACTED_" + fieldName + "_" + original.length());
        }
        if (PSEUDONYM.contains(field) || (field.equals("id") && parentName != null
                && ID_PARENTS.contains(parentName.toLowerCase(Locale.ROOT)))) {
            return pseudonymNode(node);
        }
        if (field.equals("text")) {
            String original = scalarText(node);
            return JsonNodeFactory.instance.textNode("REDACTED_TEXT_" + original.codePointCount(0, original.length())
                    + "_" + original.length());
        }
        if (field.equals("message") && parentName == null) {
            return node;
        }
        if (KEEP.contains(field)) {
            return node;
        }
        if (node.isTextual()) {
            defaultFields.add(fieldName);
            return JsonNodeFactory.instance.textNode("REDACTED_" + node.textValue().length());
        }
        if (node.isNumber()) {
            defaultFields.add(fieldName);
            return node;
        }
        return node;
    }

    private JsonNode pseudonymNode(JsonNode node) {
        String pseudonym = pseudonymize(scalarText(node));
        return node.isNumber() ? mapper.readTree(pseudonym) : JsonNodeFactory.instance.textNode(pseudonym);
    }

    private static String redactTemplateValue(JsonNode node) {
        String type = node.isTextual() ? "string" : node.getNodeType().name().toLowerCase(Locale.ROOT);
        String original = node.isTextual() ? node.textValue() : node.toString();
        return "REDACTED_" + type + "_" + original.length();
    }

    private String redactParameters(String encoded) {
        if (encoded.isEmpty()) {
            return encoded;
        }
        List<String> result = new ArrayList<>();
        for (String pair : encoded.split("&", -1)) {
            int separator = pair.indexOf('=');
            String encodedName = separator < 0 ? pair : pair.substring(0, separator);
            String encodedValue = separator < 0 ? "" : pair.substring(separator + 1);
            String name = decode(encodedName);
            String value = decode(encodedValue);
            JsonNode redacted = redactNode(JsonNodeFactory.instance.textNode(value), name, null);
            String redactedValue = redacted.textValue();
            result.add(encode(name) + (separator < 0 ? "" : "=" + encode(redactedValue)));
        }
        return String.join("&", result);
    }

    private static String redactSignature(String signature) {
        int separator = signature.indexOf('=');
        String prefix = separator < 0 ? "" : signature.substring(0, separator + 1);
        String value = separator < 0 ? signature : signature.substring(separator + 1);
        return prefix + "REDACTED_SIG_" + value.length();
    }

    private static String scalarText(JsonNode node) {
        return node.isTextual() ? node.textValue() : node.toString();
    }

    private static int firstDigitIndex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
