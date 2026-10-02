package io.github.hoangluongtran0309.vietnam.vietqr.confirmation;

import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentConfirmationTest {

    private static final Instant AT = Instant.parse("2026-10-02T03:15:00Z");

    @Test
    void keepsValuesAndBuildsIdempotencyKey() {
        PaymentConfirmation confirmation = confirmation("970436", 150_000, "DH12345 Thanh toan", Map.of("ref", "FT123"));

        assertThat(confirmation.provider()).isEqualTo("sepay");
        assertThat(confirmation.amount()).isEqualTo(150_000);
        assertThat(confirmation.content()).isEqualTo("DH12345 Thanh toan");
        assertThat(confirmation.attributes()).containsEntry("ref", "FT123");
        assertThat(confirmation.idempotencyKey()).isEqualTo("sepay:TX-1");
    }

    @Test
    void optionalValuesHaveSafeDefaults() {
        PaymentConfirmation confirmation = confirmation(null, 1, null, null);

        assertThat(confirmation.bankBin()).isNull();
        assertThat(confirmation.content()).isEmpty();
        assertThat(confirmation.attributes()).isEmpty();
    }

    @Test
    void attributesAreAnImmutableCopy() {
        Map<String, String> source = new HashMap<>(Map.of("ref", "FT123"));
        PaymentConfirmation confirmation = confirmation(null, 1, "", source);

        source.put("ref", "changed");

        assertThat(confirmation.attributes()).containsEntry("ref", "FT123");
        assertThatThrownBy(() -> confirmation.attributes().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingOrBlankRequiredValues() {
        assertThatThrownBy(() -> new PaymentConfirmation(null, "TX-1", null, "0123456789", 1, "", AT, Map.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("provider");
        assertThatThrownBy(() -> new PaymentConfirmation("sepay", " ", null, "0123456789", 1, "", AT, Map.of()))
                .isInstanceOf(VietQrException.class).hasMessage("transactionId must not be blank");
        assertThatThrownBy(() -> new PaymentConfirmation("sepay", "TX-1", null, "", 1, "", AT, Map.of()))
                .isInstanceOf(VietQrException.class).hasMessage("accountNumber must not be blank");
        assertThatThrownBy(() -> new PaymentConfirmation("sepay", "TX-1", null, "0123456789", 1, "", null, Map.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("occurredAt");
    }

    @Test
    void rejectsColonInProviderSoKeysCannotCollide() {
        // Otherwise ("a:", "b") and ("a", ":b") would both produce the key "a::b".
        assertThatThrownBy(() -> new PaymentConfirmation("a:", "b", null, "0123456789", 1, "", AT, Map.of()))
                .isInstanceOf(VietQrException.class).hasMessage("provider must not contain ':': a:");
        assertThat(new PaymentConfirmation("a", ":b", null, "0123456789", 1, "", AT, Map.of()).idempotencyKey())
                .isEqualTo("a::b");
    }

    @Test
    void rejectsInvalidAmountAndBin() {
        assertThatThrownBy(() -> confirmation(null, 0, "", Map.of()))
                .isInstanceOf(VietQrException.class).hasMessage("amount must be positive: 0");
        assertThatThrownBy(() -> confirmation("9704", 1, "", Map.of()))
                .isInstanceOf(VietQrException.class).hasMessage("bankBin must be exactly 6 digits: 9704");
    }

    @Test
    void listenerCanIgnoreRedeliveriesByIdempotencyKey() {
        Set<String> seen = new HashSet<>();
        List<Long> processed = new ArrayList<>();
        PaymentConfirmationListener listener = confirmation -> {
            if (seen.add(confirmation.idempotencyKey())) {
                processed.add(confirmation.amount());
            }
        };

        PaymentConfirmation confirmation = confirmation("970436", 150_000, "DH12345", Map.of());
        listener.onPaymentConfirmed(confirmation);
        listener.onPaymentConfirmed(confirmation);

        assertThat(processed).containsExactly(150_000L);
    }

    private static PaymentConfirmation confirmation(String bankBin, long amount, String content,
                                                    Map<String, String> attributes) {
        return new PaymentConfirmation("sepay", "TX-1", bankBin, "0123456789", amount, content, AT, attributes);
    }
}
