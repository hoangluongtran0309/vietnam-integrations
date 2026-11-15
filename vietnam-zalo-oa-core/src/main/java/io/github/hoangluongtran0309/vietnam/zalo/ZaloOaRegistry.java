package io.github.hoangluongtran0309.vietnam.zalo;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-neutral registry of configured Zalo Official Accounts.
 *
 * <p>Every account owns its token manager and messaging clients. Accounts belonging to the same application share
 * one OAuth client, while all accounts use the token store supplied to the builder.
 */
public final class ZaloOaRegistry {

    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final List<ZaloOa> accounts;
    private final Map<String, ZaloOa> accountsByAlias;
    private final Map<String, ZaloOa> accountsByOaId;
    private final Map<String, ZaloAccountConfig> configsByAlias;
    private final Map<String, ZaloOAuthClient> oauthClientsByAlias;
    private final ZaloOa defaultOa;

    private ZaloOaRegistry(Builder builder) {
        List<ZaloAccountConfig> configs = validate(builder);
        Map<String, String> secretsByAppId = validateUniqueAccounts(configs);

        Map<String, ZaloOAuthClient> oauthClientsByAppId = new LinkedHashMap<>();
        secretsByAppId.forEach((appId, secretKey) -> oauthClientsByAppId.put(appId,
                new HttpZaloOAuthClient(appId, secretKey, builder.httpClient, builder.oauthEndpoint,
                        builder.requestTimeout, builder.clock)));

        List<ZaloOa> orderedAccounts = new ArrayList<>();
        Map<String, ZaloOa> aliases = new LinkedHashMap<>();
        Map<String, ZaloOa> oaIds = new LinkedHashMap<>();
        Map<String, ZaloAccountConfig> accountConfigs = new LinkedHashMap<>();
        Map<String, ZaloOAuthClient> accountOauthClients = new LinkedHashMap<>();
        for (ZaloAccountConfig config : configs) {
            ZaloOAuthClient oauthClient = oauthClientsByAppId.get(config.appId());
            ZaloTokenManager tokenManager = new ZaloTokenManager(
                    config.oaId(), builder.tokenStore, oauthClient, builder.clock);
            ZaloOa account = new DefaultZaloOa(config.alias(), config.oaId(), config.appId(), tokenManager,
                    new HttpZaloOaClient(tokenManager, builder.httpClient, builder.messagingEndpoint,
                            builder.requestTimeout),
                    new HttpZaloZbsClient(tokenManager, builder.httpClient, builder.zbsEndpoint,
                            builder.requestTimeout));
            orderedAccounts.add(account);
            aliases.put(config.alias(), account);
            oaIds.put(config.oaId(), account);
            accountConfigs.put(config.alias(), config);
            accountOauthClients.put(config.alias(), oauthClient);
        }
        this.accounts = List.copyOf(orderedAccounts);
        this.accountsByAlias = Collections.unmodifiableMap(aliases);
        this.accountsByOaId = Collections.unmodifiableMap(oaIds);
        this.configsByAlias = Collections.unmodifiableMap(accountConfigs);
        this.oauthClientsByAlias = Collections.unmodifiableMap(accountOauthClients);
        this.defaultOa = resolveDefault(builder.defaultAccount, aliases, orderedAccounts);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(List<ZaloAccountConfig> accounts, ZaloTokenStore tokenStore) {
        return new Builder().accounts(accounts).tokenStore(tokenStore);
    }

    /** Returns the account with the requested alias. */
    public ZaloOa oa(String alias) {
        ZaloOa account = accountsByAlias.get(alias);
        if (account == null) {
            throw unknown("alias", alias);
        }
        return account;
    }

    /** Returns the account with the requested Official Account id. */
    public ZaloOa byOaId(String oaId) {
        return findByOaId(oaId).orElseThrow(() -> unknown("OA id", oaId));
    }

    /** Finds an account by Official Account id. */
    public Optional<ZaloOa> findByOaId(String oaId) {
        return Optional.ofNullable(accountsByOaId.get(oaId));
    }

    /** Returns an immutable list in configuration order. */
    public List<ZaloOa> accounts() {
        return accounts;
    }

    /** Returns the configured default account, or the sole account when exactly one exists. */
    public Optional<ZaloOa> defaultOa() {
        return Optional.ofNullable(defaultOa);
    }

    ZaloAccountConfig accountConfig(String alias) {
        return configsByAlias.get(alias);
    }

    ZaloOAuthClient oauthClient(String alias) {
        return oauthClientsByAlias.get(alias);
    }

    private ZaloException unknown(String lookup, String value) {
        return new ZaloException("No Zalo OA account found for " + lookup + " '" + value
                + "'. Available aliases: " + accountsByAlias.keySet());
    }

    private static List<ZaloAccountConfig> validate(Builder builder) {
        Objects.requireNonNull(builder.accounts, "accounts");
        if (builder.accounts.isEmpty()) {
            throw new IllegalArgumentException("accounts must not be empty");
        }
        Objects.requireNonNull(builder.tokenStore, "tokenStore");
        Objects.requireNonNull(builder.httpClient, "httpClient");
        Objects.requireNonNull(builder.oauthEndpoint, "oauthEndpoint");
        Objects.requireNonNull(builder.messagingEndpoint, "messagingEndpoint");
        Objects.requireNonNull(builder.zbsEndpoint, "zbsEndpoint");
        Objects.requireNonNull(builder.clock, "clock");
        requirePositive(builder.requestTimeout, "requestTimeout");
        requirePositive(builder.lockTimeout, "lockTimeout");
        if (builder.lockTimeout.compareTo(builder.requestTimeout) <= 0) {
            throw new IllegalArgumentException("lockTimeout must be greater than requestTimeout");
        }
        return List.copyOf(builder.accounts);
    }

    private static Map<String, String> validateUniqueAccounts(List<ZaloAccountConfig> configs) {
        Map<String, ZaloAccountConfig> aliases = new LinkedHashMap<>();
        Map<String, ZaloAccountConfig> oaIds = new LinkedHashMap<>();
        Map<String, String> secretsByAppId = new LinkedHashMap<>();
        for (ZaloAccountConfig config : configs) {
            Objects.requireNonNull(config, "account");
            if (aliases.putIfAbsent(config.alias(), config) != null) {
                throw new IllegalArgumentException("Duplicate Zalo account alias: " + config.alias());
            }
            if (oaIds.putIfAbsent(config.oaId(), config) != null) {
                throw new IllegalArgumentException("Duplicate Zalo OA id: " + config.oaId());
            }
            String existingSecret = secretsByAppId.putIfAbsent(config.appId(), config.secretKey());
            if (existingSecret != null && !existingSecret.equals(config.secretKey())) {
                throw new IllegalArgumentException("Accounts with appId '" + config.appId()
                        + "' must use the same secretKey");
            }
        }
        return secretsByAppId;
    }

    private static ZaloOa resolveDefault(String defaultAccount, Map<String, ZaloOa> aliases,
                                         List<ZaloOa> orderedAccounts) {
        if (defaultAccount != null) {
            ZaloOa account = aliases.get(defaultAccount);
            if (account == null) {
                throw new IllegalArgumentException("defaultAccount '" + defaultAccount
                        + "' does not match a configured alias. Available aliases: " + aliases.keySet());
            }
            return account;
        }
        return orderedAccounts.size() == 1 ? orderedAccounts.get(0) : null;
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    /** Builder for a registry and its per-account clients. */
    public static final class Builder {
        private List<ZaloAccountConfig> accounts = List.of();
        private ZaloTokenStore tokenStore;
        private HttpClient httpClient = HttpClient.newHttpClient();
        private URI oauthEndpoint = HttpZaloOAuthClient.DEFAULT_ENDPOINT;
        private URI messagingEndpoint = HttpZaloOaClient.DEFAULT_ENDPOINT;
        private URI zbsEndpoint = HttpZaloZbsClient.DEFAULT_ENDPOINT;
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        private Duration lockTimeout = JdbcZaloTokenStore.DEFAULT_LOCK_TIMEOUT;
        private Clock clock = Clock.systemUTC();
        private String defaultAccount;

        public Builder accounts(List<ZaloAccountConfig> accounts) {
            this.accounts = accounts;
            return this;
        }

        public Builder tokenStore(ZaloTokenStore tokenStore) {
            this.tokenStore = tokenStore;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder oauthEndpoint(URI oauthEndpoint) {
            this.oauthEndpoint = oauthEndpoint;
            return this;
        }

        public Builder messagingEndpoint(URI messagingEndpoint) {
            this.messagingEndpoint = messagingEndpoint;
            return this;
        }

        public Builder zbsEndpoint(URI zbsEndpoint) {
            this.zbsEndpoint = zbsEndpoint;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder lockTimeout(Duration lockTimeout) {
            this.lockTimeout = lockTimeout;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder defaultAccount(String defaultAccount) {
            this.defaultAccount = defaultAccount;
            return this;
        }

        public ZaloOaRegistry build() {
            return new ZaloOaRegistry(this);
        }
    }

    private record DefaultZaloOa(
            String alias,
            String oaId,
            String appId,
            ZaloTokenManager tokenManager,
            ZaloOaClient messaging,
            ZaloZbsClient zbs) implements ZaloOa {
    }
}
