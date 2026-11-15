package io.github.hoangluongtran0309.vietnam.zalo;

/** Clients and token lifecycle for one configured Zalo Official Account. */
public interface ZaloOa {

    String alias();

    String oaId();

    String appId();

    ZaloTokenManager tokenManager();

    ZaloOaClient messaging();

    ZaloZbsClient zbs();
}
