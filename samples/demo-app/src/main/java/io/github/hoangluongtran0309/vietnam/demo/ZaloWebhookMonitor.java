package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloWebhookEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicReference;

@RestController
class ZaloWebhookMonitor {

    private final AtomicReference<ZaloWebhookEvent> lastEvent = new AtomicReference<>();

    @EventListener
    void receive(ZaloWebhookEvent event) {
        lastEvent.set(event);
    }

    @GetMapping("/zalo/webhook/last")
    ZaloWebhookEvent last() {
        return lastEvent.get();
    }
}
