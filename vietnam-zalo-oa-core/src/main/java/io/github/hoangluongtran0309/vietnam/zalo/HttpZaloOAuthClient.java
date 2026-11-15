package io.github.hoangluongtran0309.vietnam.zalo;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Zalo OA OAuth v4 client. Requests are never retried because refresh tokens are single-use. */
public final class HttpZaloOAuthClient implements ZaloOAuthClient {

    public static final URI DEFAULT_ENDPOINT = URI.create("https://oauth.zaloapp.com/v4/oa/access_token");
    // TODO(v0.2): verify expired/revoked refresh-token error codes — source: OAuth OA v4 documentation.
    private static final Set<Integer> REAUTHORIZATION_REQUIRED_ERROR_CODES = Set.of(-14014);

    private final String appId;
    private final String secretKey;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final Clock clock;

    public HttpZaloOAuthClient(String appId, String secretKey) {
        this(appId, secretKey, HttpClient.newHttpClient(), DEFAULT_ENDPOINT, Duration.ofSeconds(10), Clock.systemUTC());
    }

    public HttpZaloOAuthClient(String appId, String secretKey, HttpClient httpClient, URI endpoint,
                               Duration requestTimeout, Clock clock) {
        this.appId = require(appId, "appId");
        this.secretKey = require(secretKey, "secretKey");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ZaloToken exchangeAuthorizationCode(String code, String codeVerifier) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("code", require(code, "code"));
        form.put("app_id", appId);
        form.put("grant_type", "authorization_code");
        form.put("code_verifier", require(codeVerifier, "codeVerifier"));
        return request(form, "Zalo OAuth code exchange");
    }

    @Override
    public ZaloToken refresh(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("refresh_token", require(refreshToken, "refreshToken"));
        form.put("app_id", appId);
        form.put("grant_type", "refresh_token");
        try {
            return request(form, "Zalo OAuth token refresh");
        } catch (ZaloApiException e) {
            if (e.apiErrorCode() != null && REAUTHORIZATION_REQUIRED_ERROR_CODES.contains(e.apiErrorCode())) {
                throw new ZaloReauthorizationRequiredException(e);
            }
            throw e;
        }
    }

    private ZaloToken request(Map<String, String> form, String operation) {
        String body = form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = ZaloHttpSupport.request(endpoint, requestTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("secret_key", secretKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        JsonObject json = ZaloHttpSupport.send(httpClient, request, operation);
        String accessToken = requiredJson(json, "access_token", operation);
        String newRefreshToken = requiredJson(json, "refresh_token", operation);
        String expires = requiredJson(json, "expires_in", operation);
        final long expiresIn;
        try {
            expiresIn = Long.parseLong(expires);
        } catch (NumberFormatException e) {
            throw new ZaloApiException(operation, 200, null, "invalid expires_in");
        }
        Instant expiresAt = clock.instant().plusSeconds(expiresIn);
        return new ZaloToken(accessToken, newRefreshToken, expiresAt);
    }

    private static String requiredJson(JsonObject json, String name, String operation) {
        String value = ZaloHttpSupport.string(json, name);
        if (value == null || value.isBlank()) {
            throw new ZaloApiException(operation, 200, null, "missing " + name);
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
