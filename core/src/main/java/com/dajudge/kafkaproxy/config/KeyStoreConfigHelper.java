package com.dajudge.kafkaproxy.config;

import com.dajudge.proxybase.certs.KeyStoreConfig;

import java.util.Optional;

final class KeyStoreConfigHelper {
    private static final String SUFFIX_PASSWORD = "PASSWORD";
    private static final String SUFFIX_REFRESH_MSECS = "REFRESH_MSECS";
    private static final String SUFFIX_TYPE = "TYPE";
    private static final String SUFFIX_LOCATION = "LOCATION";
    private static final String SUFFIX_PASSWORD_LOCATION = SUFFIX_PASSWORD + "_" + SUFFIX_LOCATION;
    private static final String QUALIFIER_TRUSTSTORE = "TRUSTSTORE_";
    private static final String QUALIFIER_KEYSTORE = "KEYSTORE_";
    private static final String QUALIFIER_KEY = "KEY_";
    private static final String DEFAULT_TYPE = "jks";
    private static final int DEFAULT_REFRESH_MSECS = 30000;

    private KeyStoreConfigHelper() {
    }

    static KeyStoreConfig requiredKeyStoreConfig(final Environment environment, final String prefix) {
        return loadKeyStoreConfig(environment, prefix, true).orElseThrow(IllegalStateException::new);
    }

    static Optional<KeyStoreConfig> optionalKeyStoreConfig(final Environment environment, final String prefix) {
        return loadKeyStoreConfig(environment, prefix, false);
    }

    static KeyStoreConfig requiredTrustStoreConfig(final Environment environment, final String prefix) {
        return loadTrustStoreConfig(environment, prefix, true).orElseThrow(IllegalStateException::new);
    }

    static Optional<KeyStoreConfig> optionalTrustStoreConfig(final Environment environment, final String prefix) {
        return loadTrustStoreConfig(environment, prefix, false);
    }

    private static Optional<KeyStoreConfig> loadTrustStoreConfig(
            final Environment environment,
            final String prefix,
            final boolean required
    ) {
        final String truststorePrefix = prefix + QUALIFIER_TRUSTSTORE;
        if (required) {
            environment.requiredString(truststorePrefix + SUFFIX_LOCATION);
        }
        return environment.optionalString(truststorePrefix + SUFFIX_LOCATION).map(path -> new KeyStoreConfig(
                path,
                environment.optionalString(truststorePrefix + SUFFIX_PASSWORD).orElse("").toCharArray(),
                environment.optionalString(truststorePrefix + SUFFIX_PASSWORD_LOCATION).orElse(null),
                null,
                null,
                environment.optionalString(truststorePrefix + SUFFIX_TYPE).orElse(DEFAULT_TYPE),
                environment.optionalInt(truststorePrefix + SUFFIX_REFRESH_MSECS).orElse(DEFAULT_REFRESH_MSECS)
        ));
    }

    private static Optional<KeyStoreConfig> loadKeyStoreConfig(
            final Environment environment,
            final String prefix,
            final boolean required
    ) {
        final String keystorePrefix = prefix + QUALIFIER_KEYSTORE;
        final String keyPrefix = prefix + QUALIFIER_KEY;
        if (required) {
            environment.requiredString(keystorePrefix + SUFFIX_LOCATION);
        }
        return environment.optionalString(keystorePrefix + SUFFIX_LOCATION).map(path -> new KeyStoreConfig(
                path,
                environment.optionalString(keystorePrefix + SUFFIX_PASSWORD).orElse("").toCharArray(),
                environment.optionalString(keystorePrefix + SUFFIX_PASSWORD_LOCATION).orElse(null),
                environment.optionalString(keyPrefix + SUFFIX_PASSWORD).orElse("").toCharArray(),
                environment.optionalString(keyPrefix + SUFFIX_PASSWORD_LOCATION).orElse(null),
                environment.optionalString(keystorePrefix + SUFFIX_TYPE).orElse(DEFAULT_TYPE),
                environment.optionalInt(keystorePrefix + SUFFIX_REFRESH_MSECS).orElse(DEFAULT_REFRESH_MSECS)
        ));
    }
}
