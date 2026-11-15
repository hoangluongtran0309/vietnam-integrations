package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class ZaloPkceTest {

    @Test
    void producesRfc7636S256Challenge() {
        assertThat(ZaloPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
                .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void generatesOneTimeValuesInZaloFormat() {
        SecureRandom deterministic = new SecureRandom("test-seed".getBytes(StandardCharsets.UTF_8));
        ZaloPkce first = ZaloPkce.generate(deterministic);
        ZaloPkce second = ZaloPkce.generate(deterministic);

        assertThat(first.codeVerifier()).hasSize(43).matches("[A-Za-z0-9]{43}");
        assertThat(first.codeVerifier()).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*");
        assertThat(first.codeChallenge()).isEqualTo(ZaloPkce.challenge(first.codeVerifier()));
        assertThat(first.state()).matches("[A-Za-z0-9_-]{32}");
        assertThat(second).isNotEqualTo(first);
    }
}
