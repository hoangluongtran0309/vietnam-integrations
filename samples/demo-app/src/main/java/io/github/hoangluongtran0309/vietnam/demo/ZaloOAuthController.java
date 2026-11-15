package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloPkce;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloToken;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import io.github.hoangluongtran0309.vietnam.zalo.autoconfigure.ZaloOaProperties;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@ConditionalOnBean({ZaloOAuthClient.class, ZaloTokenStore.class})
class ZaloOAuthController {

    private static final String VERIFIER = ZaloOAuthController.class.getName() + ".verifier";
    private static final String STATE = ZaloOAuthController.class.getName() + ".state";

    private final ZaloOaProperties properties;
    private final ZaloOAuthClient oauthClient;
    private final ZaloTokenStore tokenStore;
    private final String redirectUri;

    ZaloOAuthController(ZaloOaProperties properties, ZaloOAuthClient oauthClient, ZaloTokenStore tokenStore,
                        @Value("${demo.zalo.redirect-uri}") String redirectUri) {
        this.properties = properties;
        this.oauthClient = oauthClient;
        this.tokenStore = tokenStore;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/zalo/oauth/start")
    Map<String, String> start(HttpSession session) {
        ZaloPkce pkce = ZaloPkce.generate();
        session.setAttribute(VERIFIER, pkce.codeVerifier());
        session.setAttribute(STATE, pkce.state());
        String authorizationUrl = "https://oauth.zaloapp.com/v4/oa/permission"
                + "?app_id=" + encode(properties.getAppId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&code_challenge=" + encode(pkce.codeChallenge())
                + "&state=" + encode(pkce.state());
        return Map.of("authorizationUrl", authorizationUrl, "state", pkce.state(),
                "codeChallenge", pkce.codeChallenge());
    }

    @GetMapping("/zalo/oauth/callback")
    Map<String, String> callback(@RequestParam String code, @RequestParam("oa_id") String oaId,
                                 @RequestParam String state, HttpSession session) {
        String expectedState = (String) session.getAttribute(STATE);
        String verifier = (String) session.getAttribute(VERIFIER);
        session.removeAttribute(STATE);
        session.removeAttribute(VERIFIER);
        if (expectedState == null || verifier == null || !constantTimeEquals(expectedState, state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OAuth state");
        }
        if (properties.getOaId() != null && !properties.getOaId().isBlank()
                && !properties.getOaId().equals(oaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth callback returned an unexpected OA id");
        }
        ZaloToken token = oauthClient.exchangeAuthorizationCode(code, verifier);
        tokenStore.save(oaId, token);
        return Map.of("status", "authorized", "oaId", oaId,
                "accessTokenExpiresAt", token.accessTokenExpiresAt().toString());
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
