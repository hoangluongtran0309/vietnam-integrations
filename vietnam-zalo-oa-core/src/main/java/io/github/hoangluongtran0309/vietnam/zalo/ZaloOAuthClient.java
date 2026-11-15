package io.github.hoangluongtran0309.vietnam.zalo;

/**
 * Port for the Zalo OAuth (v4) endpoints.
 */
public interface ZaloOAuthClient {

    ZaloToken exchangeAuthorizationCode(String code, String codeVerifier);

    ZaloToken refresh(String refreshToken);
}
