package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrRequest;

/**
 * Convenience facade that fills in the configured default beneficiary.
 */
public class VietQrGenerator {

    private final VietQrEncoder encoder;
    private final VietQrProperties properties;

    public VietQrGenerator(VietQrEncoder encoder, VietQrProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** Dynamic QR for the default beneficiary. */
    public String forAmount(long amount, String purpose) {
        return encoder.encode(defaults().amount(amount).purpose(purpose).build());
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
