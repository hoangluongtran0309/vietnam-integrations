package io.github.hoangluongtran0309.vietnam.einvoice.signature;

import java.util.List;

/**
 * The checks of the invoice signatures in document order.
 *
 * @param checks signature checks, defensively copied
 */
public record SignatureVerification(List<SignatureCheck> checks) {
    public SignatureVerification {
        checks = List.copyOf(checks);
    }
}
