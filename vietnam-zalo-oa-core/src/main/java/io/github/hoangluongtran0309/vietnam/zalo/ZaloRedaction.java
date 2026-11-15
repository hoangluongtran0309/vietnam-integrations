package io.github.hoangluongtran0309.vietnam.zalo;

final class ZaloRedaction {

    private ZaloRedaction() {
    }

    static String secret(String value) {
        return "<redacted:" + value.length() + ">";
    }

    static String recipient(String value) {
        String normalized = value.startsWith("+") ? value.substring(1) : value;
        String prefix = normalized.startsWith("84") ? "84" : "";
        String suffix = normalized.length() > 3 ? normalized.substring(normalized.length() - 3) : "";
        return prefix + "******" + suffix;
    }
}
