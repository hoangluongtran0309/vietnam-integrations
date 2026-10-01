package io.github.hoangluongtran0309.vietnam.zalo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZaloTokenManagerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void returnsCachedTokenWhenValid() {
        InMemoryZaloTokenStore store = new InMemoryZaloTokenStore();
        store.save("oa", new ZaloToken("access-1", "refresh-1", NOW.plus(Duration.ofHours(1))));
        CountingOAuth oauth = new CountingOAuth();

        assertThat(new ZaloTokenManager("oa", store, oauth, clock).accessToken()).isEqualTo("access-1");
        assertThat(oauth.refreshCalls.get()).isZero();
    }

    @Test
    void refreshesAndPersistsRotatedRefreshToken() {
        InMemoryZaloTokenStore store = new InMemoryZaloTokenStore();
        store.save("oa", new ZaloToken("access-1", "refresh-1", NOW.plus(Duration.ofMinutes(1))));
        CountingOAuth oauth = new CountingOAuth();

        assertThat(new ZaloTokenManager("oa", store, oauth, clock).accessToken()).isEqualTo("access-new");
        assertThat(store.load("oa")).get().extracting(ZaloToken::refreshToken).isEqualTo("refresh-new");
    }

    @Test
    void failsClearlyWhenNotAuthorized() {
        assertThatThrownBy(() -> new ZaloTokenManager("oa", new InMemoryZaloTokenStore(), new CountingOAuth(), clock)
                .accessToken())
                .isInstanceOf(ZaloException.class)
                .hasMessageContaining("OAuth");
    }

    private final class CountingOAuth implements ZaloOAuthClient {
        final AtomicInteger refreshCalls = new AtomicInteger();

        @Override
        public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ZaloToken refresh(String refreshToken) {
            refreshCalls.incrementAndGet();
            return new ZaloToken("access-new", "refresh-new", NOW.plus(Duration.ofHours(1)));
        }
    }
}
