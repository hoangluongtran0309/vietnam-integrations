package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Objects;

/**
 * Input for generating a VietQR payload.
 *
 * @param bankBin       6-digit NAPAS BIN of the beneficiary bank (e.g. "970436" for Vietcombank)
 * @param accountNumber beneficiary account or card number
 * @param serviceCode   transfer type
 * @param amount        amount in VND; {@code null} produces a static QR (payer enters the amount)
 * @param purpose       transfer content; {@code null} for none. Vietnamese diacritics are stripped.
 */
public record VietQrRequest(
        String bankBin,
        String accountNumber,
        ServiceCode serviceCode,
        Long amount,
        String purpose) {

    public VietQrRequest {
        Objects.requireNonNull(bankBin, "bankBin");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(serviceCode, "serviceCode");
        if (!bankBin.matches("\\d{6}")) {
            throw new VietQrException("bankBin must be exactly 6 digits: " + bankBin);
        }
        if (!accountNumber.matches("[0-9A-Za-z]{1,19}")) {
            throw new VietQrException("accountNumber must be 1-19 alphanumeric characters");
        }
        if (amount != null && amount <= 0) {
            throw new VietQrException("amount must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isDynamic() {
        return amount != null;
    }

    public static final class Builder {
        private String bankBin;
        private String accountNumber;
        private ServiceCode serviceCode = ServiceCode.TO_ACCOUNT;
        private Long amount;
        private String purpose;

        public Builder bankBin(String bankBin) {
            this.bankBin = bankBin;
            return this;
        }

        public Builder bank(VietQrBank bank) {
            this.bankBin = bank.bin();
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder serviceCode(ServiceCode serviceCode) {
            this.serviceCode = serviceCode;
            return this;
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        public VietQrRequest build() {
            return new VietQrRequest(bankBin, accountNumber, serviceCode, amount, purpose);
        }
    }
}
