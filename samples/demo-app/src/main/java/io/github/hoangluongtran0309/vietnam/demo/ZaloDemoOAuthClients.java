package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.zalo.ZaloOAuthClient;

import java.util.Map;

final class ZaloDemoOAuthClients {

    private final Map<String, ZaloOAuthClient> clientsByAlias;

    ZaloDemoOAuthClients(Map<String, ZaloOAuthClient> clientsByAlias) {
        this.clientsByAlias = Map.copyOf(clientsByAlias);
    }

    ZaloOAuthClient forAccount(String alias) {
        ZaloOAuthClient client = clientsByAlias.get(alias);
        if (client == null) {
            throw new IllegalArgumentException("No OAuth client configured for Zalo account '" + alias + "'");
        }
        return client;
    }
}
