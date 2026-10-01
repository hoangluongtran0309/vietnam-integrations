package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Arrays;
import java.util.Optional;

/**
 * Common Vietnamese banks and their NAPAS BINs.
 *
 * <p>TODO(v0.1): verify every BIN against the official NAPAS / VietQR bank list and add the remaining banks.
 * Users can always pass a raw BIN via {@link VietQrRequest.Builder#bankBin(String)}.
 */
public enum VietQrBank {

    VIETCOMBANK("VCB", "970436"),
    VIETINBANK("ICB", "970415"),
    BIDV("BIDV", "970418"),
    AGRIBANK("VBA", "970405"),
    TECHCOMBANK("TCB", "970407"),
    MB_BANK("MB", "970422"),
    ACB("ACB", "970416"),
    VPBANK("VPB", "970432"),
    TPBANK("TPB", "970423"),
    SACOMBANK("STB", "970403");

    private final String shortCode;
    private final String bin;

    VietQrBank(String shortCode, String bin) {
        this.shortCode = shortCode;
        this.bin = bin;
    }

    public String shortCode() {
        return shortCode;
    }

    public String bin() {
        return bin;
    }

    public static Optional<VietQrBank> fromBin(String bin) {
        return Arrays.stream(values()).filter(b -> b.bin.equals(bin)).findFirst();
    }
}
