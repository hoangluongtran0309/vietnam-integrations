package io.github.hoangluongtran0309.vietnam.demo.capture;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapturingHttpClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private WireMockServer server;

    @BeforeEach
    void startWireMock() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopWireMock() {
        server.stop();
    }

    @Test
    void preservesRequestAndResponseBodiesAndCapturesEveryHttpOutcome(@TempDir Path base) throws Exception {
        CaptureSession session = session(base);
        HttpClient client = new CapturingHttpClient(HttpClient.newHttpClient(), session);
        stub("/json", 200, "{\"message_id\":\"message-original\",\"message\":\"Thành công\"}");
        stub("/form", 200, "{\"error\":0}");
        stub("/empty", 204, "");
        stub("/bad", 400, "{\"error\":-1,\"message\":\"Bad request\"}");
        stub("/failed", 500, "{\"error\":-2,\"message\":\"Server error\"}");

        String vietnameseJson = "{\"recipient\":{\"user_id\":\"user-original\"},"
                + "\"message\":{\"text\":\"Xin chào Việt Nam 👋\"}}";
        HttpResponse<byte[]> json = client.send(request("/json?access_token=query-secret", vietnameseJson,
                        "application/json"), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(json.statusCode()).isEqualTo(200);
        assertThat(new String(json.body(), StandardCharsets.UTF_8))
                .isEqualTo("{\"message_id\":\"message-original\",\"message\":\"Thành công\"}");
        server.verify(postRequestedFor(urlEqualTo("/json?access_token=query-secret"))
                .withRequestBody(equalTo(vietnameseJson)));

        String form = "refresh_token=refresh-original&app_id=app-original&grant_type=refresh_token";
        assertThat(client.send(request("/form", form, "application/x-www-form-urlencoded"),
                HttpResponse.BodyHandlers.ofString()).body()).isEqualTo("{\"error\":0}");
        assertThat(client.send(request("/empty", "", "application/json"),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(204);
        assertThat(client.send(request("/bad", "{\"phone\":\"84901234567\"}", "application/json"),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
        assertThat(client.sendAsync(request("/failed", "{\"template_id\":\"12345\"}", "application/json"),
                HttpResponse.BodyHandlers.ofString()).join().statusCode()).isEqualTo(500);
        session.close();

        List<Path> files;
        try (var stream = Files.list(session.directory())) {
            files = stream.sorted().toList();
        }
        assertThat(files).hasSize(6);
        files.forEach(file -> assertThat(mapper.readTree(file.toFile())).isNotNull());
        String allCaptures = String.join("\n", files.stream().map(this::read).toList());
        assertThat(allCaptures).contains("REDACTED_TEXT_", "Bad request", "Server error", "defaultFields")
                .doesNotContain("query-secret", "user-original", "Xin chào Việt Nam", "message-original",
                        "refresh-original", "app-original", "84901234567", "12345");
    }

    @Test
    void rethrowsIoFailureAndWritesCaptureWithoutExceptionMessage(@TempDir Path base) throws Exception {
        CaptureSession session = session(base);
        HttpClient client = new CapturingHttpClient(HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(100)).build(), session);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:1/private"))
                .POST(HttpRequest.BodyPublishers.ofString("access_token=must-not-leak"))
                .build();

        assertThatThrownBy(() -> client.send(request, HttpResponse.BodyHandlers.ofString()))
                .isInstanceOf(IOException.class);

        Path capture;
        try (var stream = Files.list(session.directory())) {
            capture = stream.filter(path -> path.getFileName().toString().contains("-http-"))
                    .findFirst().orElseThrow();
        }
        String content = read(capture);
        assertThat(content).contains("\"error\"").doesNotContain("must-not-leak", "Connection refused");
    }

    @Test
    void fullyRedactsNonJsonResponseContainingEquals(@TempDir Path base) throws Exception {
        CaptureSession session = session(base);
        HttpClient client = new CapturingHttpClient(HttpClient.newHttpClient(), session);
        String responseBody = "token abc123 expires=5";
        stub("/html", 200, "text/html;charset=UTF-8", responseBody);

        HttpResponse<String> response = client.send(request("/html", "", "application/json"),
                HttpResponse.BodyHandlers.ofString());
        session.close();

        assertThat(response.body()).isEqualTo(responseBody);
        assertThat(read(httpCapture(session))).contains("REDACTED_" + responseBody.length())
                .doesNotContain("abc123", "token abc123");
    }

    @Test
    void fullyRedactsMalformedJsonResponseContainingAccessToken(@TempDir Path base) throws Exception {
        CaptureSession session = session(base);
        HttpClient client = new CapturingHttpClient(HttpClient.newHttpClient(), session);
        String responseBody = "{\"access_token=abc123";
        stub("/malformed-json", 200, "application/json;charset=UTF-8", responseBody);

        HttpResponse<String> response = client.send(request("/malformed-json", "", "application/json"),
                HttpResponse.BodyHandlers.ofString());
        session.close();

        assertThat(response.body()).isEqualTo(responseBody);
        assertThat(read(httpCapture(session))).contains("REDACTED_" + responseBody.length())
                .doesNotContain("abc123", "token abc123");
    }

    @Test
    void rejectsCaptureDirectoryInsideGitTree(@TempDir Path base) throws Exception {
        Files.createDirectory(base.resolve(".git"));

        assertThatThrownBy(() -> session(base.resolve("nested")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo.zalo.capture-dir")
                .hasMessageContaining(".git");
    }

    @Test
    void rejectsCaptureDirectoryThatIsAFile(@TempDir Path base) throws Exception {
        Path file = Files.writeString(base.resolve("capture.json"), "not a directory");

        assertThatThrownBy(() -> session(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo.zalo.capture-dir")
                .hasMessageContaining("directory");
    }

    private CaptureSession session(Path base) {
        return new CaptureSession(base.toString(), mapper, Clock.systemUTC(),
                List.of(new CaptureSession.Account("main", "oa-real", "app-real")));
    }

    private void stub(String path, int status, String body) {
        stub(path, status, "application/json;charset=UTF-8", body);
    }

    private void stub(String path, int status, String contentType, String body) {
        server.stubFor(post(urlPathEqualTo(path)).willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", contentType).withBody(body)));
    }

    private HttpRequest request(String path, String body, String contentType) {
        return HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                .header("Content-Type", contentType)
                .header("access_token", "header-secret")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private Path httpCapture(CaptureSession session) throws IOException {
        try (var stream = Files.list(session.directory())) {
            return stream.filter(path -> path.getFileName().toString().contains("-http-"))
                    .findFirst().orElseThrow();
        }
    }
}
