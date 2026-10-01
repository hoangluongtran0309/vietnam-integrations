package io.github.hoangluongtran0309.vietnam.einvoice;

/**
 * Seller or buyer. {@code taxCode} may be null for individual buyers.
 */
public record Party(String name, String taxCode, String address, String email, String phone) {
}
