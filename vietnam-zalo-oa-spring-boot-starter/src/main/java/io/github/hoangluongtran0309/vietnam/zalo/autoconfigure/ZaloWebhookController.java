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

    @PostMapping(path = "${vietnam.zalo-oa.webhook.path:/webhooks/zalo}", consumes = "application/json")
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-ZEvent-Signature", required = false) String signature) {
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
            event = parser.parse(rawBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        publisher.publishEvent(event);
        return ResponseEntity.ok().build();
    }
}
