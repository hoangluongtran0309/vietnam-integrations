package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "vietnam.zalo-oa")
public class ZaloOaProperties {

    /** Whether Zalo OA auto-configuration is enabled. */
    private boolean enabled = true;

    /** Zalo application id. */
    private String appId;

    /** Zalo application secret key used by OAuth. */
    private String secretKey;

    /** Official Account id. */
    private String oaId;

    /** OA secret key used to verify webhook signatures. */
    private String oaSecretKey;

    /** Token store implementation: memory for one instance or jdbc for shared PostgreSQL storage. */
    private TokenStoreType tokenStore = TokenStoreType.MEMORY;

    /** Timeout for establishing outbound HTTP connections. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Timeout for each outbound Zalo API request. Requests are never retried automatically. */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /** JDBC token-store settings. */
    private Jdbc jdbc = new Jdbc();

    /** Optional webhook endpoint settings. */
    private Webhook webhook = new Webhook();

    public enum TokenStoreType { MEMORY, JDBC }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getOaId() { return oaId; }
    public void setOaId(String oaId) { this.oaId = oaId; }
    public String getOaSecretKey() { return oaSecretKey; }
    public void setOaSecretKey(String oaSecretKey) { this.oaSecretKey = oaSecretKey; }
    public TokenStoreType getTokenStore() { return tokenStore; }
    public void setTokenStore(TokenStoreType tokenStore) { this.tokenStore = tokenStore; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public Jdbc getJdbc() { return jdbc; }
    public void setJdbc(Jdbc jdbc) { this.jdbc = jdbc; }
    public Webhook getWebhook() { return webhook; }
    public void setWebhook(Webhook webhook) { this.webhook = webhook; }

    @Override
    public String toString() {
        return "ZaloOaProperties[enabled=" + enabled + ", appId=" + appId
                + ", secretKey=" + redact(secretKey) + ", oaId=" + oaId
                + ", oaSecretKey=" + redact(oaSecretKey) + ", tokenStore=" + tokenStore
                + ", connectTimeout=" + connectTimeout + ", requestTimeout=" + requestTimeout
                + ", jdbc=" + jdbc + ", webhook=" + webhook + "]";
    }

    private static String redact(String value) {
        return "<redacted:" + (value == null ? 0 : value.length()) + ">";
    }

    public static class Jdbc {
        /**
         * Maximum time to wait for the PostgreSQL token row lock. This must be longer than request-timeout because
         * the instance holding the lock may wait for Zalo until its HTTP request times out.
         */
        private Duration lockTimeout = Duration.ofSeconds(15);

        public Duration getLockTimeout() { return lockTimeout; }
        public void setLockTimeout(Duration lockTimeout) { this.lockTimeout = lockTimeout; }

        @Override
        public String toString() {
            return "Jdbc[lockTimeout=" + lockTimeout + "]";
        }
    }

    public static class Webhook {
        /** Whether the servlet webhook controller is exposed. Disabled by default. */
        private boolean enabled;

        /** HTTP path receiving Zalo OA webhook POST requests. */
        private String path = "/webhooks/zalo";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        @Override
        public String toString() {
            return "Webhook[enabled=" + enabled + ", path=" + path + "]";
        }
    }
}
