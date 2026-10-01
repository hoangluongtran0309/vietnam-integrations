package io.github.hoangluongtran0309.vietnam.zalo;

public class ZaloException extends RuntimeException {

    public ZaloException(String message) {
        super(message);
    }

    public ZaloException(String message, Throwable cause) {
        super(message, cause);
    }
}
