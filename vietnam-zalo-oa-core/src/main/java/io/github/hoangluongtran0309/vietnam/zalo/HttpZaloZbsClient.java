package io.github.hoangluongtran0309.vietnam.zalo;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** java.net.http implementation of ZBS Template Message delivery by phone number. */
public final class HttpZaloZbsClient implements ZaloZbsClient {

    public static final URI DEFAULT_ENDPOINT = URI.create("https://business.openapi.zalo.me/message/template");

    private final ZaloTokenManager tokenManager;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration requestTimeout;

    public HttpZaloZbsClient(ZaloTokenManager tokenManager) {
        this(tokenManager, HttpClient.newHttpClient(), DEFAULT_ENDPOINT, Duration.ofSeconds(10));
    }

    public HttpZaloZbsClient(ZaloTokenManager tokenManager, HttpClient httpClient, URI endpoint,
                             Duration requestTimeout) {
        this.tokenManager = Objects.requireNonNull(tokenManager, "tokenManager");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    @Override
    public ZaloMessageResult sendTemplateByPhone(ZbsPhoneTemplateRequest request) {
        Objects.requireNonNull(request, "request");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", request.phone());
        body.put("template_id", request.templateId());
        body.put("template_data", request.templateData());
        body.put("tracking_id", request.trackingId());
        HttpRequest httpRequest = ZaloHttpSupport.request(endpoint, requestTimeout)
                .header("Content-Type", "application/json")
                .header("access_token", tokenManager.accessToken())
                .POST(HttpRequest.BodyPublishers.ofString(ZaloHttpSupport.GSON.toJson(body)))
                .build();
        JsonObject json = ZaloHttpSupport.send(httpClient, httpRequest, "send ZBS template by phone");
        return HttpZaloOaClient.result(json, request.phone(), "msg_id");
    }
}
