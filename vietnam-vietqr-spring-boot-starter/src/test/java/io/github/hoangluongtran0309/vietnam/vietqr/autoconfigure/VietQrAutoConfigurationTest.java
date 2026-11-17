package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableLookup;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PaymentReconciler;
import io.github.hoangluongtran0309.vietnam.vietqr.reference.PaymentReferenceFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VietQrAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VietQrAutoConfiguration.class));

    @Test
    void registersBeansByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(VietQrEncoder.class);
            assertThat(ctx).hasSingleBean(VietQrGenerator.class);
            assertThat(ctx).hasSingleBean(QrImageRenderer.class);
            assertThat(ctx).doesNotHaveBean(PaymentReferenceFormat.class);
            assertThat(ctx).doesNotHaveBean(PaymentReconciler.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("vietnam.vietqr.enabled=false")
                .withPropertyValues("vietnam.vietqr.reference.prefix=DH")
                .withBean(PayableLookup.class, VietQrAutoConfigurationTest::emptyLookup)
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(VietQrGenerator.class);
                    assertThat(ctx).doesNotHaveBean(PaymentReferenceFormat.class);
                    assertThat(ctx).doesNotHaveBean(PaymentReconciler.class);
                });
    }

    @Test
    void generatesWithConfiguredDefaults() {
        runner.withPropertyValues("vietnam.vietqr.bank-bin=970436", "vietnam.vietqr.account-number=0123456789")
                .run(ctx -> assertThat(ctx.getBean(VietQrGenerator.class).forAmount(10_000, "DH1"))
                        .contains("970436").contains("0123456789"));
    }

    @Test
    void userBeanWins() {
        VietQrEncoder custom = new VietQrEncoder();
        runner.withBean(VietQrEncoder.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(VietQrEncoder.class)).isSameAs(custom));
    }

    @Test
    void referencePrefixRegistersFormatWithoutReconciler() {
        runner.withPropertyValues("vietnam.vietqr.reference.prefix=DH")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(PaymentReferenceFormat.class);
                    assertThat(ctx).doesNotHaveBean(PaymentReconciler.class);
                    assertThat(ctx.getBean(VietQrProperties.class).getReference().getPrefix()).isEqualTo("DH");
                });
    }

    @Test
    void lookupWithoutReferencePrefixDoesNotRegisterReconciliationBeans() {
        runner.withBean(PayableLookup.class, VietQrAutoConfigurationTest::emptyLookup)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(PayableLookup.class);
                    assertThat(ctx).doesNotHaveBean(PaymentReferenceFormat.class);
                    assertThat(ctx).doesNotHaveBean(PaymentReconciler.class);
                });
    }

    @Test
    void referencePrefixAndLookupRegisterReconciler() {
        runner.withPropertyValues("vietnam.vietqr.reference.prefix=DH")
                .withBean(PayableLookup.class, VietQrAutoConfigurationTest::emptyLookup)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(PaymentReferenceFormat.class);
                    assertThat(ctx).hasSingleBean(PaymentReconciler.class);
                });
    }

    @Test
    void customReferenceFormatWins() {
        PaymentReferenceFormat custom = PaymentReferenceFormat.of("XX");
        runner.withPropertyValues("vietnam.vietqr.reference.prefix=DH")
                .withBean(PaymentReferenceFormat.class, () -> custom)
                .withBean(PayableLookup.class, VietQrAutoConfigurationTest::emptyLookup)
                .run(ctx -> {
                    assertThat(ctx.getBean(PaymentReferenceFormat.class)).isSameAs(custom);
                    assertThat(ctx).hasSingleBean(PaymentReconciler.class);
                });
    }

    @Test
    void customReferenceFormatCanEnableReconcilerWithoutProperty() {
        PaymentReferenceFormat custom = PaymentReferenceFormat.of("XX");
        runner.withBean(PaymentReferenceFormat.class, () -> custom)
                .withBean(PayableLookup.class, VietQrAutoConfigurationTest::emptyLookup)
                .run(ctx -> assertThat(ctx).hasSingleBean(PaymentReconciler.class));
    }

    @Test
    void customReconcilerWins() {
        PaymentReferenceFormat format = PaymentReferenceFormat.of("DH");
        PayableLookup lookup = emptyLookup();
        PaymentReconciler custom = new PaymentReconciler(format, lookup);

        runner.withPropertyValues("vietnam.vietqr.reference.prefix=DH")
                .withBean(PayableLookup.class, () -> lookup)
                .withBean(PaymentReconciler.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(PaymentReconciler.class)).isSameAs(custom));
    }

    @Test
    void failsFastWithExactPropertyNameWhenReferencePrefixIsInvalid() {
        for (String value : new String[] {"dh", ""}) {
            runner.withPropertyValues("vietnam.vietqr.reference.prefix=" + value)
                    .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
                            .isInstanceOf(VietQrException.class)
                            .hasMessageContaining("vietnam.vietqr.reference.prefix"));
        }
    }

    @Test
    void generatesForValidReference() {
        runner.withPropertyValues(
                        "vietnam.vietqr.bank-bin=970436",
                        "vietnam.vietqr.account-number=0123456789",
                        "vietnam.vietqr.reference.prefix=DH")
                .run(ctx -> {
                    PaymentReferenceFormat format = ctx.getBean(PaymentReferenceFormat.class);
                    String reference = format.encode("12345");
                    String payload = ctx.getBean(VietQrGenerator.class).forReference(150_000, reference);

                    assertThat(ctx.getBean(VietQrDecoder.class).decode(payload))
                            .satisfies(decoded -> {
                                assertThat(decoded.amount()).isEqualTo(150_000);
                                assertThat(decoded.purpose()).isEqualTo(reference);
                            });
                });
    }

    @Test
    void rejectsReferenceWithInvalidChecksumOrWrongPrefix() {
        runner.withPropertyValues(
                        "vietnam.vietqr.bank-bin=970436",
                        "vietnam.vietqr.account-number=0123456789",
                        "vietnam.vietqr.reference.prefix=DH")
                .run(ctx -> {
                    VietQrGenerator generator = ctx.getBean(VietQrGenerator.class);
                    assertThatThrownBy(() -> generator.forReference(150_000, "DH123456"))
                            .isInstanceOf(VietQrException.class)
                            .hasMessageContaining("vietnam.vietqr.reference.prefix");
                    assertThatThrownBy(() -> generator.forReference(150_000, "XX12345V"))
                            .isInstanceOf(VietQrException.class)
                            .hasMessageContaining("vietnam.vietqr.reference.prefix");
                });
    }

    @Test
    void forReferenceRequiresConfiguredFormat() {
        runner.withPropertyValues(
                        "vietnam.vietqr.bank-bin=970436",
                        "vietnam.vietqr.account-number=0123456789")
                .run(ctx -> assertThatThrownBy(
                                () -> ctx.getBean(VietQrGenerator.class).forReference(150_000, "DH123455"))
                        .isInstanceOf(VietQrException.class)
                        .hasMessageContaining("vietnam.vietqr.reference.prefix"));
    }

    private static PayableLookup emptyLookup() {
        return reference -> Optional.empty();
    }
}
