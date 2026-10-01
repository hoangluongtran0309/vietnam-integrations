package io.github.hoangluongtran0309.vietnam.einvoice;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes calls to the configured provider.
 */
public class EInvoiceService {

    private final Map<String, EInvoiceProvider> providers;
    private final String defaultProvider;

    public EInvoiceService(List<EInvoiceProvider> providers, String defaultProvider) {
        this.providers = providers.stream().collect(Collectors.toMap(EInvoiceProvider::id, Function.identity()));
        this.defaultProvider = defaultProvider;
    }

    public IssuedInvoice issue(Invoice invoice) {
        return provider(defaultProvider).issue(invoice);
    }

    public EInvoiceProvider provider(String id) {
        EInvoiceProvider provider = providers.get(id);
        if (provider == null) {
            throw new EInvoiceException("No e-invoice provider '" + id + "'. Available: " + providers.keySet());
        }
        return provider;
    }
}
