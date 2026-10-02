package io.github.hoangluongtran0309.vietnam.vietqr;

/**
 * QR code error correction level. Higher levels survive more damage, or a logo drawn over the code, at the cost of a
 * denser symbol.
 */
public enum QrErrorCorrection {

    /** Recovers about 7% of the symbol. */
    L,

    /** Recovers about 15% of the symbol. */
    M,

    /** Recovers about 25% of the symbol. */
    Q,

    /** Recovers about 30% of the symbol. */
    H
}
