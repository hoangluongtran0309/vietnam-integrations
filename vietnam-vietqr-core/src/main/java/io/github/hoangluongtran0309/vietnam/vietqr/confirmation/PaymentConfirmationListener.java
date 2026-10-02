package io.github.hoangluongtran0309.vietnam.vietqr.confirmation;

/**
 * Receives incoming transfers reported by a payment confirmation adapter. Adapters for providers such as SePay and
 * Casso are planned for 0.4; this interface is the contract they will call.
 *
 * <p>Implementations must follow these rules:
 * <ul>
 *   <li><b>At least once.</b> The same transfer may be delivered more than once, for example when a provider retries
 *       a webhook. Process each {@link PaymentConfirmation#idempotencyKey()} only once.</li>
 *   <li><b>Incoming only.</b> Adapters deliver money received on the merchant's account, never outgoing transfers.</li>
 *   <li><b>Unmatched transfers are normal.</b> A payer may transfer without scanning the QR code, edit the content or
 *       send a different amount. Record such transfers for manual review instead of throwing.</li>
 *   <li><b>Throwing means failure.</b> An exception tells the adapter the confirmation was not processed, so it can
 *       report the failure to the provider for a retry where the provider supports one.</li>
 *   <li><b>Concurrency.</b> Calls may arrive concurrently on several threads.</li>
 * </ul>
 */
@FunctionalInterface
public interface PaymentConfirmationListener {

    void onPaymentConfirmed(PaymentConfirmation confirmation);
}
