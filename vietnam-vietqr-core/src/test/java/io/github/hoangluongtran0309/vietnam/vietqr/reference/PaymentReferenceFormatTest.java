package io.github.hoangluongtran0309.vietnam.vietqr.reference;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentReferenceFormatTest {

    private final PaymentReferenceFormat format = PaymentReferenceFormat.of("DH");

    @Test
    void encodesFixedLuhnModNVector() {
        // DH12345 from right: 2*5 + 1*4 + 2*3 + 1*2 + 2*1 + 1*H(17) + 2*D(13) = 67.
        // All products are below 36, so the check value is (36 - 67 mod 36) mod 36 = 5.
        assertThat(format.encode("12345")).isEqualTo("DH123455");
        assertThat(format.encode("ab12")).isEqualTo("DHAB12O");
    }

    @Test
    void rejectsChangedCharacter() {
        assertThat(format.isValid("DH123455")).isTrue();
        assertThat(format.isValid("DH123465")).isFalse();
    }

    @Test
    void detectsAdjacentTransposition() {
        String reference = format.encode("12345");
        String transposed = "DH21345" + reference.charAt(reference.length() - 1);

        assertThat(format.isValid(transposed)).isFalse();
    }

    @Test
    void documentsZeroAndLastAlphabetCharacterTranspositionLimit() {
        String reference = format.encode("10Z2");
        String transposed = reference.substring(0, 3) + "Z0" + reference.substring(5);

        assertThat(reference).isEqualTo("DH10Z2L");
        assertThat(format.isValid(transposed)).isTrue();
    }

    @Test
    void uppercasesBodyButRequiresCanonicalReference() {
        String reference = format.encode("abc123");

        assertThat(reference).startsWith("DHABC123");
        assertThat(format.isValid(reference.toLowerCase())).isFalse();
    }

    @Test
    void validatesPrefixAndBodyBoundaries() {
        assertThatThrownBy(() -> PaymentReferenceFormat.of("D"))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("prefix");
        assertThatThrownBy(() -> PaymentReferenceFormat.of("dh"))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("prefix");
        assertThatThrownBy(() -> format.encode(""))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("body");
        assertThatThrownBy(() -> format.encode("ABC-123"))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("body");
        assertThatThrownBy(() -> format.encode("ß"))
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("body");
        assertThat(format.encode("A".repeat(20))).hasSize(23);
    }

    @Test
    void enforcesCompleteReferenceLengthLimit() {
        PaymentReferenceFormat longPrefixFormat = PaymentReferenceFormat.of("ABCDEF");

        assertThat(longPrefixFormat.encode("A".repeat(18))).hasSize(25);
        assertThatThrownBy(() -> longPrefixFormat.encode("A".repeat(19)))
                .isInstanceOf(VietQrException.class)
                .hasMessage("reference length must not exceed 25 characters: 26");
    }

    @ParameterizedTest(name = "extracts references from bank content #{index}")
    @CsvFileSource(resources = "/reference/bank-contents.csv", numLinesToSkip = 1, nullValues = "<none>")
    void extractsReferencesFromBankContent(String content, String expected) {
        List<String> expectedReferences = expected == null ? List.of() : Arrays.asList(expected.split("\\|"));

        assertThat(format.extract(content)).containsExactlyElementsOf(expectedReferences);
    }

    @Test
    void returnsAllChecksumValidLengthsFromOneAttachedRun() {
        // DH0T is valid for body "0"; the same attached run extended by "2" is valid for body "0T".
        String content = "XXDH0T2YY";

        assertThat(format.extract(content)).containsExactly("DH0T", "DH0T2");
    }
}
