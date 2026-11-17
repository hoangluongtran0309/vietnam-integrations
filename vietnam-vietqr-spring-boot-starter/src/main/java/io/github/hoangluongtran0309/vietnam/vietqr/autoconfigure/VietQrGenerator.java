package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrRequest;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;

/**
 * Convenience facade that fills in the configured default beneficiary.
 */
public class VietQrGenerator {

    private final VietQrEncoder encoder;
    private final VietQrProperties properties;
    private final PaymentReferenceFormat referenceFormat;

    public VietQrGenerator(VietQrEncoder encoder, VietQrProperties properties) {
        this(encoder, properties, null);
    }

    public VietQrGenerator(
            VietQrEncoder encoder,
            VietQrProperties properties,
            PaymentReferenceFormat referenceFormat) {
        this.encoder = encoder;
        this.properties = properties;
        this.referenceFormat = referenceFormat;
    }

    /** Dynamic QR for the default beneficiary. */
    public String forAmount(long amount, String purpose) {
        return encoder.encode(defaults().amount(amount).purpose(purpose).build());
    }

    /**
     * Dynamic QR whose purpose is a reference validated against the configured payment-reference format.
     *
     * @throws VietQrException if no format is configured or the reference is invalid for that format
     */
    public String forReference(long amount, String reference) {
        if (referenceFormat == null) {
            throw new VietQrException(
                    "Payment reference format not configured: set vietnam.vietqr.reference.prefix");
        }
        if (!referenceFormat.isValid(reference)) {
            throw new VietQrException(
                    "Invalid payment reference for vietnam.vietqr.reference.prefix: " + reference);
        }
        return forAmount(amount, reference);
    }

    /** Static QR (payer enters the amount) for the default beneficiary. */
    public String staticQr() {
        return encoder.encode(defaults().build());
    }

    /** Fully custom request; ignores configured defaults. */
    public String generate(VietQrRequest request) {
        return encoder.encode(request);
    }

    private VietQrRequest.Builder defaults() {
        if (properties.getBankBin() == null || properties.getAccountNumber() == null) {
            throw new VietQrException(
                    "Default beneficiary not configured: set vietnam.vietqr.bank-bin and vietnam.vietqr.account-number");
        }
        return VietQrRequest.builder()
                .bankBin(properties.getBankBin())
                .accountNumber(properties.getAccountNumber())
                .serviceCode(properties.getServiceCode());
    }
}
