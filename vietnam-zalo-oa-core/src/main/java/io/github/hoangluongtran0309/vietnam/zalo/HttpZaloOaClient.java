package io.github.hoangluongtran0309.vietnam.zalo;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** java.net.http implementation of OA consultation messaging v3. */
public final class HttpZaloOaClient implements ZaloOaClient {

    public static final URI DEFAULT_ENDPOINT = URI.create("https://openapi.zalo.me/v3.0/oa/message/cs");

    private final ZaloTokenManager tokenManager;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration requestTimeout;

    public HttpZaloOaClient(ZaloTokenManager tokenManager) {
        this(tokenManager, HttpClient.newHttpClient(), DEFAULT_ENDPOINT, Duration.ofSeconds(10));
    }

    public HttpZaloOaClient(ZaloTokenManager tokenManager, HttpClient httpClient, URI endpoint,
                            Duration requestTimeout) {
        this.tokenManager = Objects.requireNonNull(tokenManager, "tokenManager");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    @Override
    public ZaloMessageResult sendConsultationText(ZaloConsultationTextRequest request) {
        Objects.requireNonNull(request, "request");
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("text", request.text());
        if (request.quoteMessageId() != null) {
            message.put("quote_message_id", request.quoteMessageId());
        }
        Map<String, Object> body = Map.of(
                "recipient", Map.of("user_id", request.userId()),
                "message", message);
        HttpRequest httpRequest = ZaloHttpSupport.request(endpoint, requestTimeout)
                .header("Content-Type", "application/json")
                .header("access_token", tokenManager.accessToken())
                .POST(HttpRequest.BodyPublishers.ofString(ZaloHttpSupport.GSON.toJson(body)))
                .build();
        JsonObject json = ZaloHttpSupport.send(httpClient, httpRequest, "send OA consultation text");
        return result(json, request.userId(), "message_id");
    }

    static ZaloMessageResult result(JsonObject response, String recipient, String idField) {
        if (!response.has("data") || !response.get("data").isJsonObject()) {
            throw new ZaloApiException("parse message response", 200, null, "missing data");
        }
        JsonObject data = response.getAsJsonObject("data");
        String messageId = ZaloHttpSupport.string(data, idField);
        String sentTime = ZaloHttpSupport.string(data, "sent_time");
        if (messageId == null || sentTime == null) {
            throw new ZaloApiException("parse message response", 200, null, "missing message id or sent_time");
        }
        try {
            return new ZaloMessageResult(messageId, recipient, Instant.ofEpochMilli(Long.parseLong(sentTime)));
        } catch (NumberFormatException e) {
            throw new ZaloApiException("parse message response", 200, null, "invalid sent_time");
        }
    }
}
