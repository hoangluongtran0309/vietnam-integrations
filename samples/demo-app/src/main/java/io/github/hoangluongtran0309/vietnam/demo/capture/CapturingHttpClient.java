package io.github.hoangluongtran0309.vietnam.demo.capture;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

final class CapturingHttpClient extends HttpClient {

    private final HttpClient delegate;
    private final CaptureSession session;

    CapturingHttpClient(HttpClient delegate, CaptureSession session) {
        this.delegate = delegate;
        this.session = session;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        byte[] requestBody = collectRequestBody(request);
        long started = System.nanoTime();
        try {
            HttpResponse<byte[]> response = delegate.send(request, HttpResponse.BodyHandlers.ofByteArray());
            capture(request, requestBody, response, null, elapsedMillis(started));
            return convert(response, responseBodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            capture(request, requestBody, null, e, elapsedMillis(started));
            throw e;
        } catch (IOException | RuntimeException e) {
            capture(request, requestBody, null, e, elapsedMillis(started));
            throw e;
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                             HttpResponse.BodyHandler<T> responseBodyHandler) {
        return sendAsync(request, responseBodyHandler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                             HttpResponse.BodyHandler<T> responseBodyHandler,
                                                             HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        final byte[] requestBody;
        try {
            requestBody = collectRequestBody(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        } catch (IOException | RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        long started = System.nanoTime();
        CompletableFuture<HttpResponse<byte[]>> future = delegate.sendAsync(request,
                HttpResponse.BodyHandlers.ofByteArray());
        return future.handle((response, failure) -> {
            if (failure != null) {
                Throwable cause = unwrap(failure);
                capture(request, requestBody, null, cause, elapsedMillis(started));
                throw new CompletionException(cause);
            }
            capture(request, requestBody, response, null, elapsedMillis(started));
            return convert(response, responseBodyHandler);
        });
    }

    private byte[] collectRequestBody(HttpRequest request) throws IOException, InterruptedException {
        Optional<HttpRequest.BodyPublisher> publisher = request.bodyPublisher();
        if (publisher.isEmpty()) {
            return new byte[0];
        }
        BodyCollector collector = new BodyCollector();
        publisher.get().subscribe(collector);
        try {
            return collector.body().get();
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("Cannot collect request body", cause);
        }
    }

    private void capture(HttpRequest request, byte[] requestBody, HttpResponse<byte[]> response,
                         Throwable failure, long durationMillis) {
        String suffix = request.method() + "-" + request.uri().getPath();
        try {
            CaptureRedactor redactor = session.redactor();
            ObjectNode root = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
            root.put("capturedAt", session.now().toString());
            root.put("method", request.method());
            root.put("url", redactedUrl(request.uri(), redactor));
            root.set("requestHeaders", toNode(redactor.redactHeaders(request.headers().map())));
            root.set("requestBody", redactBody(requestBody, request.headers().firstValue("Content-Type").orElse(""),
                    redactor, suffix));
            if (response != null) {
                root.put("status", response.statusCode());
                root.set("responseHeaders", toNode(redactor.redactHeaders(response.headers().map())));
                root.set("responseBody", redactBody(response.body(),
                        response.headers().firstValue("Content-Type").orElse(""), redactor, suffix));
            }
            if (failure != null) {
                root.put("error", failure.getClass().getName());
            }
            root.put("durationMillis", durationMillis);
            session.writeCapture("http", suffix, root);
        } catch (RuntimeException e) {
            session.warn(e.getClass().getSimpleName(), suffix);
        }
    }

    private JsonNode redactBody(byte[] body, String contentType, CaptureRedactor redactor, String filename) {
        if (body.length == 0) {
            return tools.jackson.databind.node.JsonNodeFactory.instance.textNode("");
        }
        String text = new String(body, StandardCharsets.UTF_8);
        String normalizedContentType = contentType.toLowerCase(java.util.Locale.ROOT);
        if (normalizedContentType.contains("application/x-www-form-urlencoded")) {
            return tools.jackson.databind.node.JsonNodeFactory.instance.textNode(redactor.redactForm(text));
        }
        try {
            return redactor.redactJson(body);
        } catch (RuntimeException invalidJson) {
            if (normalizedContentType.contains("json")) {
                session.warn(invalidJson.getClass().getSimpleName(), filename);
            }
        }
        return tools.jackson.databind.node.JsonNodeFactory.instance.textNode("REDACTED_" + text.length());
    }

    private static JsonNode toNode(Map<String, List<String>> headers) {
        ObjectNode result = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        headers.forEach((name, values) -> {
            var array = result.putArray(name);
            values.forEach(array::add);
        });
        return result;
    }

    private static String redactedUrl(URI uri, CaptureRedactor redactor) {
        String query = uri.getRawQuery();
        String base = uri.getScheme() + "://" + uri.getRawAuthority() + uri.getRawPath();
        return query == null ? base : base + "?" + redactor.redactQuery(query);
    }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }

    private static <T> HttpResponse<T> convert(HttpResponse<byte[]> source,
                                                HttpResponse.BodyHandler<T> handler) {
        HttpResponse.BodySubscriber<T> subscriber = handler.apply(new ResponseInfo(source));
        subscriber.onSubscribe(new Flow.Subscription() {
            public void request(long n) { }
            public void cancel() { }
        });
        subscriber.onNext(List.of(ByteBuffer.wrap(source.body())));
        subscriber.onComplete();
        T body = subscriber.getBody().toCompletableFuture().join();
        return new ConvertedResponse<>(source, body);
    }

    public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }
    public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }
    public Redirect followRedirects() { return delegate.followRedirects(); }
    public Optional<ProxySelector> proxy() { return delegate.proxy(); }
    public SSLContext sslContext() { return delegate.sslContext(); }
    public SSLParameters sslParameters() { return delegate.sslParameters(); }
    public Optional<Authenticator> authenticator() { return delegate.authenticator(); }
    public Version version() { return delegate.version(); }
    public Optional<Executor> executor() { return delegate.executor(); }
    public WebSocket.Builder newWebSocketBuilder() { return delegate.newWebSocketBuilder(); }

    private record ResponseInfo(HttpResponse<byte[]> response) implements HttpResponse.ResponseInfo {
        public int statusCode() { return response.statusCode(); }
        public HttpHeaders headers() { return response.headers(); }
        public Version version() { return response.version(); }
    }

    private record ConvertedResponse<T>(HttpResponse<byte[]> source, T body) implements HttpResponse<T> {
        public int statusCode() { return source.statusCode(); }
        public HttpRequest request() { return source.request(); }
        public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return source.headers(); }
        public Optional<javax.net.ssl.SSLSession> sslSession() { return source.sslSession(); }
        public URI uri() { return source.uri(); }
        public Version version() { return source.version(); }
    }

    private static final class BodyCollector implements Flow.Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();

        CompletableFuture<byte[]> body() { return body; }
        public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
        public void onNext(ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            output.writeBytes(bytes);
        }
        public void onError(Throwable throwable) { body.completeExceptionally(throwable); }
        public void onComplete() { body.complete(output.toByteArray()); }
    }
}
