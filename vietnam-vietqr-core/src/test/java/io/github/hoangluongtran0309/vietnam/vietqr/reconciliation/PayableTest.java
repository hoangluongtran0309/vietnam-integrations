package io.github.hoangluongtran0309.vietnam.vietqr.reconciliation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayableTest {

    @Test
    void keepsValuesAndAllowsUnknownBankBin() {
        Payable payable = new Payable("DH123455", 150_000, PayableStatus.OPEN, "0123456789", null);

        assertThat(payable.reference()).isEqualTo("DH123455");
        assertThat(payable.expectedAmount()).isEqualTo(150_000);
        assertThat(payable.status()).isEqualTo(PayableStatus.OPEN);
        assertThat(payable.accountNumber()).isEqualTo("0123456789");
        assertThat(payable.bankBin()).isNull();
    }

    @Test
    void rejectsInvalidValues() {
        assertThatThrownBy(() -> new Payable(" ", 1, PayableStatus.OPEN, "0123456789", null))
                .isInstanceOf(VietQrException.class)
                .hasMessage("reference must not be blank");
        assertThatThrownBy(() -> new Payable("DH123455", 0, PayableStatus.OPEN, "0123456789", null))
                .isInstanceOf(VietQrException.class)
                .hasMessage("expectedAmount must be positive: 0");
        assertThatThrownBy(() -> new Payable("DH123455", 1, null, "0123456789", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status");
        assertThatThrownBy(() -> new Payable("DH123455", 1, PayableStatus.OPEN, "", null))
                .isInstanceOf(VietQrException.class)
                .hasMessage("accountNumber must not be blank");
        assertThatThrownBy(() -> new Payable("DH123455", 1, PayableStatus.OPEN, "0123456789", "9704"))
                .isInstanceOf(VietQrException.class)
                .hasMessage("bankBin must be exactly 6 digits: 9704");
    }
}
