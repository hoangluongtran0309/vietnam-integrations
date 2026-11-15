package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookEvent;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookParser;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookVerifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class ZaloWebhookController {

    private final ZaloWebhookVerifier verifier;
    private final ZaloWebhookParser parser;
    private final ApplicationEventPublisher publisher;

    public ZaloWebhookController(ZaloWebhookVerifier verifier, ZaloWebhookParser parser,
                                 ApplicationEventPublisher publisher) {
        this.verifier = verifier;
        this.parser = parser;
        this.publisher = publisher;
    }

    // TODO(v0.2): verify Content-Type thực tế Zalo gửi — capture ở WP-2D.1.
    @PostMapping(path = "${vietnam.zalo-oa.webhook.path:/webhooks/zalo}")
    public ResponseEntity<Void> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(name = "X-ZEvent-Signature", required = false) String signature) {
        if (signature == null) {
            return ResponseEntity.status(401).build();
        }
        final boolean valid;
        try {
            valid = verifier.verify(rawBody, signature);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        if (!valid) {
            return ResponseEntity.status(401).build();
        }
        final ZaloWebhookEvent event;
        try {
            event = parser.parse(new String(rawBody, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            publisher.publishEvent(event);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
}
