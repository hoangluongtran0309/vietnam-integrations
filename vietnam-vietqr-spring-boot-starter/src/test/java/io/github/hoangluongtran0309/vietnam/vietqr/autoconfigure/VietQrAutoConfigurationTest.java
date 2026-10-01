package io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VietQrAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VietQrAutoConfiguration.class));

    @Test
    void registersBeansByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(VietQrEncoder.class);
            assertThat(ctx).hasSingleBean(VietQrGenerator.class);
            assertThat(ctx).hasSingleBean(QrImageRenderer.class);
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("vietnam.vietqr.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(VietQrGenerator.class));
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
}
