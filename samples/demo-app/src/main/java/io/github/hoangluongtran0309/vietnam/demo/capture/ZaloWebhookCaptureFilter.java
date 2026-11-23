package io.github.hoangluongtran0309.vietnam.demo.capture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ZaloWebhookCaptureFilter extends OncePerRequestFilter {

    static final int MAX_CAPTURE_BYTES = 1024 * 1024;

    private final CaptureSession session;
    private final String path;

    ZaloWebhookCaptureFilter(CaptureSession session, String path) {
        this.session = session;
        this.path = path;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals(request.getContextPath() + path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_CAPTURE_BYTES) {
            filterChain.doFilter(request, response);
            return;
        }

        Prefix prefix = readPrefix(request.getInputStream());
        if (prefix.tooLarge()) {
            filterChain.doFilter(new StreamingRequestWrapper(request, prefix.bytes(), prefix.remainder()), response);
            return;
        }

        CachedRequestWrapper wrapped = new CachedRequestWrapper(request, prefix.bytes());
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            capture(request, prefix.bytes(), response.getStatus());
        }
    }

    private void capture(HttpServletRequest request, byte[] body, int responseStatus) {
        String eventName = "unknown";
        try {
            boolean utf8Valid = isValidUtf8(body);
            JsonNode rawBody;
            if (utf8Valid) {
                try {
                    rawBody = session.redactor().redactJson(body);
                    JsonNode event = rawBody.path("event_name");
                    if (event.isTextual() && !event.textValue().isBlank()) {
                        eventName = event.textValue();
                    }
                } catch (RuntimeException invalidJson) {
                    session.warn(invalidJson.getClass().getSimpleName(), "webhook-unknown");
                    rawBody = JsonNodeFactory.instance.textNode("REDACTED_" + body.length);
                }
            } else {
                rawBody = JsonNodeFactory.instance.textNode("REDACTED_BINARY_" + body.length);
            }
            ObjectNode root = JsonNodeFactory.instance.objectNode();
            root.put("capturedAt", session.now().toString());
            root.put("method", request.getMethod());
            root.put("path", request.getRequestURI());
            root.set("headers", headers(request));
            root.set("rawBody", rawBody);
            root.put("bodyUtf8Valid", utf8Valid);
            root.put("responseStatus", responseStatus);
            session.writeCapture("webhook", eventName, root);
        } catch (RuntimeException e) {
            session.warn(e.getClass().getSimpleName(), "webhook-" + eventName);
        }
    }

    private JsonNode headers(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, java.util.Collections.list(request.getHeaders(name)));
        }
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        session.redactor().redactHeaders(headers).forEach((name, values) -> {
            var array = result.putArray(name);
            values.forEach(array::add);
        });
        return result;
    }

    private static Prefix readPrefix(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (output.size() <= MAX_CAPTURE_BYTES) {
            int allowed = Math.min(buffer.length, MAX_CAPTURE_BYTES + 1 - output.size());
            int read = input.read(buffer, 0, allowed);
            if (read < 0) {
                return new Prefix(output.toByteArray(), null, false);
            }
            output.write(buffer, 0, read);
            if (output.size() > MAX_CAPTURE_BYTES) {
                return new Prefix(output.toByteArray(), input, true);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private static boolean isValidUtf8(byte[] body) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private record Prefix(byte[] bytes, InputStream remainder, boolean tooLarge) {
    }

    private static final class CachedRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new DelegatingServletInputStream(new ByteArrayInputStream(body));
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class StreamingRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] prefix;
        private final InputStream remainder;

        private StreamingRequestWrapper(HttpServletRequest request, byte[] prefix, InputStream remainder) {
            super(request);
            this.prefix = prefix;
            this.remainder = remainder;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new DelegatingServletInputStream(new SequenceInputStream(new ByteArrayInputStream(prefix), remainder));
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class DelegatingServletInputStream extends ServletInputStream {
        private final InputStream delegate;

        private DelegatingServletInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        public int read() throws IOException { return delegate.read(); }
        public int read(byte[] bytes, int offset, int length) throws IOException {
            return delegate.read(bytes, offset, length);
        }
        public boolean isFinished() {
            try { return delegate.available() == 0; } catch (IOException e) { return true; }
        }
        public boolean isReady() { return true; }
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async reads are not supported");
        }
    }
}
