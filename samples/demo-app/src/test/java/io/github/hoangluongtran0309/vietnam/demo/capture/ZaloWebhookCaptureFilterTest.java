package io.github.hoangluongtran0309.vietnam.demo.capture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "debug=false",
        "vietnam.zalo-oa.enabled=true",
        "vietnam.zalo-oa.app-id=capture-app",
        "vietnam.zalo-oa.secret-key=test-app-secret",
        "vietnam.zalo-oa.oa-id=capture-oa",
        "vietnam.zalo-oa.oa-secret-key=test-oa-secret",
        "vietnam.zalo-oa.webhook.enabled=true"
})
class ZaloWebhookCaptureFilterTest {

    @TempDir
    static Path captureBase;

    @DynamicPropertySource
    static void captureDirectory(DynamicPropertyRegistry properties) {
        properties.add("demo.zalo.capture-dir", captureBase::toString);
    }

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ZaloWebhookCaptureFilter filter;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void capturesSuccessfulRejectedAndInvalidUtf8WebhooksWithoutChangingControllerBehavior() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).addFilters(filter).build();
        String body = "{\"app_id\":\"capture-app\",\"oa_id\":\"capture-oa\","
                + "\"sender\":{\"id\":\"real-user-id\"},\"recipient\":{\"id\":\"capture-oa\"},"
                + "\"event_name\":\"user_send_text\",\"message\":{\"msg_id\":\"real-message-id\","
                + "\"text\":\"Nội dung bí mật 👋\"},\"timestamp\":\"1000\"}";
        String validSignature = signature(body, "test-oa-secret", "1000");

        mockMvc.perform(post("/webhooks/zalo").contentType("application/json;charset=UTF-8")
                        .header("X-ZEvent-Signature", validSignature).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/webhooks/zalo").contentType("application/json")
                        .header("X-ZEvent-Signature", "mac=invalid-signature").content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/webhooks/zalo").contentType("application/octet-stream")
                        .header("X-ZEvent-Signature", "mac=invalid-binary-signature")
                        .content(new byte[] {(byte) 0xc3, (byte) 0x28}))
                .andExpect(status().isBadRequest());
        String oversized = body.substring(0, body.length() - 1) + ",\"padding\":\""
                + "x".repeat(ZaloWebhookCaptureFilter.MAX_CAPTURE_BYTES) + "\"}";
        mockMvc.perform(post("/webhooks/zalo").contentType("application/json")
                        .header("X-ZEvent-Signature", signature(oversized, "test-oa-secret", "1000"))
                        .content(oversized))
                .andExpect(status().isOk());

        List<Path> captures;
        try (var stream = Files.walk(captureBase)) {
            captures = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("-webhook-"))
                    .sorted().toList();
        }
        assertThat(captures).hasSize(3);
        List<JsonNode> json = captures.stream().map(path -> mapper.readTree(path.toFile())).toList();
        assertThat(json).extracting(node -> node.path("responseStatus").intValue())
                .containsExactly(200, 401, 400);
        assertThat(json.get(2).path("bodyUtf8Valid").booleanValue()).isFalse();
        String allCaptures = String.join("\n", captures.stream().map(this::read).toList());
        assertThat(allCaptures).contains("application/json;charset=UTF-8", "REDACTED_TEXT_", "mac=REDACTED_SIG_")
                .doesNotContain("real-user-id", "real-message-id", "Nội dung bí mật", validSignature,
                        "invalid-signature", "invalid-binary-signature");
    }

    private static String signature(String body, String secret, String timestamp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("capture-app" + body + timestamp + secret).getBytes(StandardCharsets.UTF_8));
            return "mac=" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }
}
