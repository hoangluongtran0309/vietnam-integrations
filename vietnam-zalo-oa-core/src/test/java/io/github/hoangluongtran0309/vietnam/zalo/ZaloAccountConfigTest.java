package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloAccountConfigTest {

    @Test
    void acceptsSupportedAliasBoundariesAndOptionalWebhookSecret() {
        assertThatCode(() -> new ZaloAccountConfig("a", "app", "secret", "oa", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> new ZaloAccountConfig("a".repeat(32), "app", "secret", "oa", "oa-secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidAliases() {
        assertInvalidAlias(null);
        assertInvalidAlias("");
        assertInvalidAlias("UPPER");
        assertInvalidAlias("has_underscore");
        assertInvalidAlias("a".repeat(33));
    }

    @Test
    void rejectsBlankRequiredValuesAndBlankOptionalSecret() {
        assertThatThrownBy(() -> new ZaloAccountConfig("valid", " ", "secret", "oa", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("appId");
        assertThatThrownBy(() -> new ZaloAccountConfig("valid", "app", " ", "oa", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("secretKey");
        assertThatThrownBy(() -> new ZaloAccountConfig("valid", "app", "secret", " ", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("oaId");
        assertThatThrownBy(() -> new ZaloAccountConfig("valid", "app", "secret", "oa", " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("oaSecretKey");
    }

    private static void assertInvalidAlias(String alias) {
        assertThatThrownBy(() -> new ZaloAccountConfig(alias, "app", "secret", "oa", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[a-z0-9-]{1,32}");
    }
}
