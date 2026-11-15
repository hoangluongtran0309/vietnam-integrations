package io.github.hoangluongtran0309.vietnam.zalo;

/** An HTTP or Zalo API error. Credentials and raw response bodies are deliberately not retained. */
public class ZaloApiException extends ZaloException {

    private final String operation;
    private final int httpStatus;
    private final Integer apiErrorCode;

    public ZaloApiException(String operation, int httpStatus, Integer apiErrorCode, String message) {
        this(operation, httpStatus, apiErrorCode, message, null);
    }

    protected ZaloApiException(String operation, int httpStatus, Integer apiErrorCode, String message,
                               Throwable cause) {
        super(operation + " failed" + (apiErrorCode == null ? "" : " with Zalo error " + apiErrorCode)
                + ": " + safe(message), cause);
        this.operation = operation;
        this.httpStatus = httpStatus;
        this.apiErrorCode = apiErrorCode;
    }

    public ZaloApiException(String operation, String message, Throwable cause) {
        super(operation + " failed: " + safe(message), cause);
        this.operation = operation;
        this.httpStatus = 0;
        this.apiErrorCode = null;
    }

    public String operation() { return operation; }
    public int httpStatus() { return httpStatus; }
    public Integer apiErrorCode() { return apiErrorCode; }

    private static String safe(String message) {
        if (message == null || message.isBlank()) {
            return "no error details returned";
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
