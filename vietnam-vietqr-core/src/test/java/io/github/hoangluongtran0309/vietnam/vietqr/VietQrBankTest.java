package io.github.hoangluongtran0309.vietnam.vietqr;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class VietQrBankTest {

    @Test
    void binsAreSixDigitsAndUnique() {
        assertThat(VietQrBank.values()).allSatisfy(b -> assertThat(b.bin()).matches("\\d{6}"));
        assertThat(Arrays.stream(VietQrBank.values()).map(VietQrBank::bin)).doesNotHaveDuplicates();
    }

    @Test
    void shortCodesAreUniqueIgnoringCase() {
        assertThat(Arrays.stream(VietQrBank.values()).map(b -> b.shortCode().toUpperCase(Locale.ROOT)))
                .doesNotHaveDuplicates();
    }

    @Test
    void everyBankHasADisplayName() {
        assertThat(VietQrBank.values()).allSatisfy(b -> assertThat(b.displayName()).isNotBlank());
    }

    @Test
    void lookupsRoundTrip() {
        for (VietQrBank bank : VietQrBank.values()) {
            assertThat(VietQrBank.fromBin(bank.bin())).contains(bank);
            assertThat(VietQrBank.fromShortCode(bank.shortCode())).contains(bank);
        }
    }

    @Test
    void shortCodeLookupIgnoresCase() {
        assertThat(VietQrBank.fromShortCode("vcb")).contains(VietQrBank.VIETCOMBANK);
        assertThat(VietQrBank.fromShortCode("MOMO")).contains(VietQrBank.MOMO);
    }

    @Test
    void unknownValuesAreEmpty() {
        assertThat(VietQrBank.fromBin("000000")).isEmpty();
        assertThat(VietQrBank.fromBin(null)).isEmpty();
        assertThat(VietQrBank.fromShortCode("NOPE")).isEmpty();
        assertThat(VietQrBank.fromShortCode(null)).isEmpty();
    }

    @Test
    void constantsFromBeforeTheFullListKeepTheirValues() {
        assertThat(VietQrBank.VIETCOMBANK).returns("970436", VietQrBank::bin).returns("VCB", VietQrBank::shortCode);
        assertThat(VietQrBank.VIETINBANK).returns("970415", VietQrBank::bin).returns("ICB", VietQrBank::shortCode);
        assertThat(VietQrBank.BIDV).returns("970418", VietQrBank::bin).returns("BIDV", VietQrBank::shortCode);
        assertThat(VietQrBank.AGRIBANK).returns("970405", VietQrBank::bin).returns("VBA", VietQrBank::shortCode);
        assertThat(VietQrBank.TECHCOMBANK).returns("970407", VietQrBank::bin).returns("TCB", VietQrBank::shortCode);
        assertThat(VietQrBank.MB_BANK).returns("970422", VietQrBank::bin).returns("MB", VietQrBank::shortCode);
        assertThat(VietQrBank.ACB).returns("970416", VietQrBank::bin).returns("ACB", VietQrBank::shortCode);
        assertThat(VietQrBank.VPBANK).returns("970432", VietQrBank::bin).returns("VPB", VietQrBank::shortCode);
        assertThat(VietQrBank.TPBANK).returns("970423", VietQrBank::bin).returns("TPB", VietQrBank::shortCode);
        assertThat(VietQrBank.SACOMBANK).returns("970403", VietQrBank::bin).returns("STB", VietQrBank::shortCode);
    }

    @Test
    void decodedPayloadResolvesItsBank() {
        String payload = new VietQrEncoder().encode(VietQrRequest.builder()
                .bank(VietQrBank.LPBANK)
                .accountNumber("0123456789")
                .build());

        assertThat(new VietQrDecoder().decode(payload).bank()).contains(VietQrBank.LPBANK);
    }

    @Test
    void decodedPayloadWithUnlistedBinHasNoBank() {
        String payload = new VietQrEncoder().encode(VietQrRequest.builder()
                .bankBin("970999")
                .accountNumber("0123456789")
                .build());

        assertThat(new VietQrDecoder().decode(payload).bank()).isEmpty();
    }
}
