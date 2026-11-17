package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.ServiceCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the default beneficiary used by {@link VietQrGenerator}.
 */
@ConfigurationProperties(prefix = "vietnam.vietqr")
public class VietQrProperties {

    /** Whether VietQR auto-configuration is enabled. */
    private boolean enabled = true;

    /** 6-digit NAPAS BIN of the default beneficiary bank, e.g. 970436. */
    private String bankBin;

    /** Default beneficiary account number. */
    private String accountNumber;

    /** Default transfer type. */
    private ServiceCode serviceCode = ServiceCode.TO_ACCOUNT;

    /** Optional payment-reference settings used for reconciliation. */
    private Reference reference = new Reference();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBankBin() { return bankBin; }
    public void setBankBin(String bankBin) { this.bankBin = bankBin; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public ServiceCode getServiceCode() { return serviceCode; }
    public void setServiceCode(ServiceCode serviceCode) { this.serviceCode = serviceCode; }
    public Reference getReference() { return reference; }
    public void setReference(Reference reference) { this.reference = reference == null ? new Reference() : reference; }

    public static class Reference {

        /** Application-specific prefix of 2-6 uppercase ASCII letters. */
        private String prefix;

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }
}
