package io.github.hoangluongtran0309.vietnam.vietqr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VietQrEncoderTest {

    private final VietQrEncoder encoder = new VietQrEncoder();
    private final VietQrDecoder decoder = new VietQrDecoder();

    @Test
    void crcMatchesCcittFalseCheckValue() {
        assertThat(Crc16Ccitt.computeHex("123456789")).isEqualTo("29B1");
    }

    @Test
    void encodesStaticQr() {
        String payload = encoder.encode(VietQrRequest.builder()
                .bankBin("970415")
                .accountNumber("113366668888")
                .build());

        assertThat(payload).isEqualTo(
                "00020101021138560010A0000007270126000697041501121133666688880208QRIBFTTA53037045802VN6304F443");
    }

    @Test
    void encodesDynamicQrWithNormalizedPurpose() {
        String payload = encoder.encode(VietQrRequest.builder()
                .bank(VietQrBank.VIETCOMBANK)
                .accountNumber("0123456789")
                .amount(150_000)
                .purpose("Thanh toán đơn hàng #DH12345")
                .build());

        assertThat(payload).isEqualTo(
                "00020101021238540010A00000072701240006970436011001234567890208QRIBFTTA"
                        + "530370454061500005802VN62320828Thanh toan don hang #DH1234563046E65");
    }

    @Test
    void roundTrip() {
        VietQrRequest request = VietQrRequest.builder()
                .bank(VietQrBank.MB_BANK)
                .accountNumber("9999999999")
                .amount(20_000)
                .purpose("DH001")
                .build();

        VietQrPayload decoded = decoder.decode(encoder.encode(request));

        assertThat(decoded.bankBin()).isEqualTo("970422");
        assertThat(decoded.accountNumber()).isEqualTo("9999999999");
        assertThat(decoded.amount()).isEqualTo(20_000L);
        assertThat(decoded.purpose()).isEqualTo("DH001");
        assertThat(decoded.dynamic()).isTrue();
    }

    @Test
    void rejectsTamperedPayload() {
        String payload = encoder.encode(VietQrRequest.builder().bankBin("970415").accountNumber("1").build());
        String tampered = payload.replace("970415", "970416");

        assertThatThrownBy(() -> decoder.decode(tampered))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("CRC mismatch");
    }

    @Test
    void rejectsInvalidBin() {
        assertThatThrownBy(() -> VietQrRequest.builder().bankBin("123").accountNumber("1").build())
                .isInstanceOf(VietQrException.class);
    }
}
