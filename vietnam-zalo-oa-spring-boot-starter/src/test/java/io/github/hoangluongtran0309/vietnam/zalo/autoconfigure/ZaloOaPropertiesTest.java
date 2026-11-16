package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloOaPropertiesTest {

    @Test
    void usesFifteenSecondJdbcLockTimeoutByDefault() {
        assertThat(new ZaloOaProperties().getJdbc().getLockTimeout()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void toStringRedactsApplicationAndOaSecretKeys() {
        ZaloOaProperties properties = new ZaloOaProperties();
        properties.setAppId("app-id");
        properties.setSecretKey("application-secret");
        properties.setOaId("oa-id");
        properties.setOaSecretKey("oa-secret-key");
        ZaloOaProperties.Account account = new ZaloOaProperties.Account();
        account.setOaId("second-oa");
        account.setAppId("second-app");
        account.setSecretKey("second-application-secret");
        account.setOaSecretKey("second-oa-secret");
        properties.setAccounts(Map.of("second", account));

        assertThat(properties.toString())
                .contains("appId=app-id", "secretKey=<redacted:18>", "oaId=oa-id",
                        "oaSecretKey=<redacted:13>", "second-oa", "second-app")
                .doesNotContain("application-secret", "oa-secret-key", "second-application-secret",
                        "second-oa-secret");
    }

    @Test
    void accountsUseLinkedHashMapAndPreserveInsertionOrder() {
        ZaloOaProperties properties = new ZaloOaProperties();
        LinkedHashMap<String, ZaloOaProperties.Account> accounts = new LinkedHashMap<>();
        accounts.put("cskh", new ZaloOaProperties.Account());
        accounts.put("marketing", new ZaloOaProperties.Account());

        properties.setAccounts(accounts);

        assertThat(properties.getAccounts()).isInstanceOf(LinkedHashMap.class);
        assertThat(properties.getAccounts().keySet()).containsExactly("cskh", "marketing");
    }
}
