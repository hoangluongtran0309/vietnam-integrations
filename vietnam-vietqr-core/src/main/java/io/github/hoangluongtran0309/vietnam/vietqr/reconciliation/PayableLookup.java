package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import java.util.Optional;

/**
 * Looks up application-owned payables by their payment reference.
 *
 * <p>The application implements this port. Implementations must be safe to call concurrently from multiple threads.
 */
@FunctionalInterface
public interface PayableLookup {

    /**
     * Finds the payable identified by the canonical payment reference.
     *
     * @param reference canonical payment reference extracted from bank content
     * @return the payable, or an empty optional when the reference is unknown; never {@code null}
     */
    Optional<Payable> findByReference(String reference);
}
