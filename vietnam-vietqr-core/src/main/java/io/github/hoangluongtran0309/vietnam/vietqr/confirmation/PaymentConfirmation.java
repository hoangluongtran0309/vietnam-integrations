package io.github.hoangluongtran0309.vietnam.vietqr.confirmation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * An incoming transfer to the merchant's account, reported by a payment confirmation provider (a service that watches
 * the account and notifies the merchant, such as SePay or Casso).
 *
 * @param provider      identifier of the adapter that produced this confirmation, e.g. {@code "sepay"}; must not
 *                      contain {@code :} so that {@link #idempotencyKey()} stays unambiguous
 * @param transactionId the provider's unique ID for this transfer; together with {@code provider} it identifies the
 *                      transfer across redeliveries
 * @param bankBin       6-digit NAPAS BIN of the receiving account, or {@code null} if the provider does not report it
 * @param accountNumber receiving account number
 * @param amount        amount received in VND, positive
 * @param content       transfer content as recorded by the bank, or an empty string. It may differ from the purpose
 *                      encoded in the QR code: the payer can edit it and banks may add prefixes or suffixes
 * @param occurredAt    when the transfer was recorded, as reported by the provider
 * @param attributes    provider-specific fields kept for logging and troubleshooting; never {@code null}
 */
public record PaymentConfirmation(
        String provider,
        String transactionId,
        String bankBin,
        String accountNumber,
        long amount,
        String content,
        Instant occurredAt,
        Map<String, String> attributes) {

    public PaymentConfirmation {
        requireText(provider, "provider");
        if (provider.contains(":")) {
            throw new VietQrException("provider must not contain ':': " + provider);
        }
        requireText(transactionId, "transactionId");
        requireText(accountNumber, "accountNumber");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (bankBin != null && !bankBin.matches("\\d{6}")) {
            throw new VietQrException("bankBin must be exactly 6 digits: " + bankBin);
        }
        if (amount <= 0) {
            throw new VietQrException("amount must be positive: " + amount);
        }
        content = content == null ? "" : content;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * {@code provider:transactionId}, a key for recognizing a transfer that is delivered more than once. Distinct
     * confirmations never share a key, because {@code provider} cannot contain {@code :}.
     */
    public String idempotencyKey() {
        return provider + ":" + transactionId;
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new VietQrException(name + " must not be blank");
        }
    }
}
