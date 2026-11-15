package io.github.hoangluongtran0309.vietnam.zalo;

/** A Zalo OAuth refresh failure that requires an operator to authorize the Official Account again. */
public final class ZaloReauthorizationRequiredException extends ZaloApiException {

    ZaloReauthorizationRequiredException(ZaloApiException cause) {
        super(cause.operation(), cause.httpStatus(), cause.apiErrorCode(),
                "the refresh token is no longer valid; complete the OAuth authorization flow again", cause);
    }
}
