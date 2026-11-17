package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOa;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloOaRegistry;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloPkce;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloToken;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloTokenStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "vietnam.zalo-oa", name = "enabled", havingValue = "true")
class ZaloOAuthController {

    private static final String ATTEMPT = ZaloOAuthController.class.getName() + ".attempt";

    private final ZaloOaRegistry registry;
    private final ZaloDemoOAuthClients oauthClients;
    private final ZaloTokenStore tokenStore;
    private final String redirectUri;

    ZaloOAuthController(ZaloOaRegistry registry, ZaloDemoOAuthClients oauthClients, ZaloTokenStore tokenStore,
                        @Value("${demo.zalo.redirect-uri}") String redirectUri) {
        this.registry = registry;
        this.oauthClients = oauthClients;
        this.tokenStore = tokenStore;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/zalo/oauth/start")
    Map<String, String> start(@RequestParam(required = false) String account, HttpSession session) {
        ZaloOa selected = selectAccount(account);
        ZaloPkce pkce = ZaloPkce.generate();
        session.setAttribute(ATTEMPT, new OAuthAttempt(selected.alias(), pkce.codeVerifier(), pkce.state()));
        String authorizationUrl = "https://oauth.zaloapp.com/v4/oa/permission"
                + "?app_id=" + encode(selected.appId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&code_challenge=" + encode(pkce.codeChallenge())
                + "&state=" + encode(pkce.state());
        return Map.of("account", selected.alias(), "authorizationUrl", authorizationUrl,
                "state", pkce.state(), "codeChallenge", pkce.codeChallenge());
    }

    @GetMapping("/zalo/oauth/callback")
    Map<String, String> callback(@RequestParam String code, @RequestParam("oa_id") String oaId,
                                 @RequestParam String state, HttpSession session) {
        OAuthAttempt attempt = (OAuthAttempt) session.getAttribute(ATTEMPT);
        session.removeAttribute(ATTEMPT);
        if (attempt == null || !constantTimeEquals(attempt.state, state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OAuth state");
        }
        ZaloOa selected = selectAccount(attempt.account);
        if (!selected.oaId().equals(oaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth callback returned an unexpected OA id");
        }
        ZaloToken token = oauthClients.forAccount(selected.alias())
                .exchangeAuthorizationCode(code, attempt.verifier);
        tokenStore.save(selected.oaId(), token);
        return Map.of("status", "authorized", "account", selected.alias(), "oaId", selected.oaId(),
                "accessTokenExpiresAt", token.accessTokenExpiresAt().toString());
    }

    private ZaloOa selectAccount(String account) {
        if (account == null || account.isBlank()) {
            return registry.defaultOa().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "account is required when no vietnam.zalo-oa.default-account is configured"));
        }
        try {
            return registry.oa(account);
        } catch (ZaloException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class OAuthAttempt implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String account;
        private final String verifier;
        private final String state;

        private OAuthAttempt(String account, String verifier, String state) {
            this.account = account;
            this.verifier = verifier;
            this.state = state;
        }
    }
}
