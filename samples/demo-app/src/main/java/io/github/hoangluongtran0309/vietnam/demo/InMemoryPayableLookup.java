package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Payable;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableLookup;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Demo-only, in-memory storage for synthetic checkout payables. */
@Component
final class InMemoryPayableLookup implements PayableLookup {

    private final ConcurrentMap<String, Payable> payables = new ConcurrentHashMap<>();

    void save(Payable payable) {
        Objects.requireNonNull(payable, "payable");
        Payable existing = payables.putIfAbsent(payable.reference(), payable);
        if (existing != null) {
            throw new IllegalStateException("Duplicate demo payment reference: " + payable.reference());
        }
    }

    @Override
    public Optional<Payable> findByReference(String reference) {
        return Optional.ofNullable(payables.get(reference));
    }
}
