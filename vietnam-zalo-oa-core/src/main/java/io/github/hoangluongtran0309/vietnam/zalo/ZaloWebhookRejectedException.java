package io.github.hoangluongtran0309.vietnam.zalo;

/** Indicates that a webhook could not be authenticated for a configured Official Account. */
public final class ZaloWebhookRejectedException extends ZaloException {

    public ZaloWebhookRejectedException(String message) {
        super(message);
    }
}
