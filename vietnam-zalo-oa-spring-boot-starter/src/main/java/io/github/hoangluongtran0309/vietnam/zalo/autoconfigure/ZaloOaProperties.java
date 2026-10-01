package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vietnam.zalo-oa")
public class ZaloOaProperties {

    /** Whether Zalo OA auto-configuration is enabled. */
    private boolean enabled = true;

    /** Zalo application id. */
    private String appId;

    /** Zalo application secret key. */
    private String secretKey;

    /** Official Account id. */
    private String oaId;

    /** OA secret key used to verify webhook signatures. */
    private String oaSecretKey;

    /**
     * Token store implementation: memory (single instance only) or jdbc (not implemented yet; startup fails
     * unless you define your own ZaloTokenStore bean).
     */
    private TokenStoreType tokenStore = TokenStoreType.MEMORY;

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
}
