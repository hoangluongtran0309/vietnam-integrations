package io.github.hoangluongtran0309.vietnam.zalo.autoconfigure;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookEvent;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookRejectedException;
import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookRouter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ZaloWebhookController {

    private final ZaloWebhookRouter router;
    private final ApplicationEventPublisher publisher;

    public ZaloWebhookController(ZaloWebhookRouter router, ApplicationEventPublisher publisher) {
        this.router = router;
        this.publisher = publisher;
    }

    // TODO(v0.2): verify Content-Type thực tế Zalo gửi — capture ở WP-2D.1.
    @PostMapping(path = "${vietnam.zalo-oa.webhook.path:/webhooks/zalo}")
    public ResponseEntity<Void> receive(
            @RequestBody byte[] rawBody,
            @RequestHeader(name = "X-ZEvent-Signature", required = false) String signature) {
        final ZaloWebhookEvent event;
        try {
            event = router.route(rawBody, signature);
        } catch (ZaloWebhookRejectedException e) {
            return ResponseEntity.status(401).build();
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
