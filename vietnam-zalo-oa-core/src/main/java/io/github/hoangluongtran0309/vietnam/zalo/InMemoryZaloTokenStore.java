package io.github.hoangluongtran0309.vietnam.zalo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryZaloTokenStore implements ZaloTokenStore {

    private final Map<String, ZaloToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<ZaloToken> load(String oaId) {
        return Optional.ofNullable(tokens.get(oaId));
    }

    @Override
    public void save(String oaId, ZaloToken token) {
        tokens.put(oaId, token);
    }
}
