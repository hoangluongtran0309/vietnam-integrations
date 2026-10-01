package io.github.hoangluongtran0309.vietnam.einvoice.autoconfigure;

import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceProvider;
import io.github.hoangluongtran0309.vietnam.einvoice.EInvoiceService;
import io.github.hoangluongtran0309.vietnam.einvoice.Invoice;
import io.github.hoangluongtran0309.vietnam.einvoice.InvoiceStatus;
import io.github.hoangluongtran0309.vietnam.einvoice.IssuedInvoice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EInvoiceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EInvoiceAutoConfiguration.class));

    @Test
    void backsOffWithoutProvider() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(EInvoiceService.class));
    }

    @Test
    void routesToConfiguredProvider() {
        StubProvider fake = new StubProvider("fake");
        runner.withPropertyValues("vietnam.einvoice.provider=fake")
                .withBean("fakeProvider", EInvoiceProvider.class, () -> fake)
                .withBean("otherProvider", EInvoiceProvider.class, () -> new StubProvider("other"))
                .run(ctx -> assertThat(ctx.getBean(EInvoiceService.class).provider("fake")).isSameAs(fake));
    }

    @Test
    void userServiceWins() {
        EInvoiceService custom = new EInvoiceService(List.of(), "none");
        runner.withPropertyValues("vietnam.einvoice.provider=fake")
                .withBean(EInvoiceService.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(EInvoiceService.class)).isSameAs(custom));
    }

    private record StubProvider(String id) implements EInvoiceProvider {

        @Override
        public IssuedInvoice issue(Invoice invoice) {
            return new IssuedInvoice(invoice.externalId(), id, null, null, null, null, null, InvoiceStatus.ISSUED);
        }

        @Override
        public InvoiceStatus status(String externalId) {
            return InvoiceStatus.ISSUED;
        }

        @Override
        public byte[] downloadPdf(String externalId) {
            return new byte[0];
        }
    }
}
