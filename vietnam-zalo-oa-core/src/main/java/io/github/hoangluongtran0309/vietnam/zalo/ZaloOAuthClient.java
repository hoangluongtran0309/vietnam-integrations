package io.github.hoangluongtran0309.vietnam.zalo;

/**
 * Port for the Zalo OAuth (v4) endpoints.
 * TODO(v0.2): implement with java.net.http.HttpClient; verify endpoints and token lifetimes against Zalo docs.
 */
public interface ZaloOAuthClient {

    ZaloToken exchangeAuthorizationCode(String code, String codeVerifier);

    ZaloToken refresh(String refreshToken);
}
