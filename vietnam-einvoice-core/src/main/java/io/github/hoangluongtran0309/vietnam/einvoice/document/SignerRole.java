package io.github.hoangluongtran0309.vietnam.einvoice.document;

/** Signer containers modeled by an issued invoice document. */
public enum SignerRole {
    /** Seller signature at {@code HDon/DSCKS/NBan/ds:Signature}. */
    SELLER,
    /** Buyer signature at {@code HDon/DSCKS/NMua/ds:Signature}. */
    BUYER,
    /** Tax-authority signature at {@code HDon/DSCKS/CQT/ds:Signature}. */
    TAX_AUTHORITY
}
