package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

public class InMemoryZaloTokenStore implements ZaloTokenStore {

    private final Map<String, ZaloToken> tokens = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<ZaloToken> load(String oaId) {
        return Optional.ofNullable(tokens.get(oaId));
    }

    @Override
    public void save(String oaId, ZaloToken token) {
        tokens.put(oaId, token);
    }

    @Override
    public ZaloToken updateAtomically(String oaId, UnaryOperator<ZaloToken> updater) {
        ReentrantLock lock = locks.computeIfAbsent(oaId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            ZaloToken current = Optional.ofNullable(tokens.get(oaId)).orElseThrow(() ->
                    new ZaloException("No token stored for OA " + oaId + ". Complete the OAuth authorization flow first."));
            ZaloToken updated = java.util.Objects.requireNonNull(updater.apply(current), "updated token");
            tokens.put(oaId, updated);
            return updated;
        } finally {
            lock.unlock();
        }
    }
}
