package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloOaPropertiesTest {

    @Test
    void toStringRedactsApplicationAndOaSecretKeys() {
        ZaloOaProperties properties = new ZaloOaProperties();
        properties.setAppId("app-id");
        properties.setSecretKey("application-secret");
        properties.setOaId("oa-id");
        properties.setOaSecretKey("oa-secret-key");

        assertThat(properties.toString())
                .contains("appId=app-id", "secretKey=<redacted:18>", "oaId=oa-id",
                        "oaSecretKey=<redacted:13>")
                .doesNotContain("application-secret", "oa-secret-key");
    }
}
