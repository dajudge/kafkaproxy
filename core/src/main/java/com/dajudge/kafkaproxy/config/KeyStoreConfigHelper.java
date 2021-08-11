/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dajudge.kafkaproxy.config;

import com.dajudge.proxybase.certs.KeyStoreConfig;

import java.util.Optional;

final class KeyStoreConfigHelper {
    private static final String SUFFIX_PASSWORD = "PASSWORD";
    private static final String SUFFIX_REFRESH_SECS = "REFRESH_SECS";
    private static final String SUFFIX_TYPE = "TYPE";
    private static final String SUFFIX_LOCATION = "LOCATION";
    private static final String SUFFIX_PASSWORD_LOCATION = SUFFIX_PASSWORD + "_" + SUFFIX_LOCATION;
    private static final String QUALIFIER_TRUSTSTORE = "TRUSTSTORE_";
    private static final String QUALIFIER_KEYSTORE = "KEYSTORE_";
    private static final String QUALIFIER_KEY = "KEY_";
    private static final String DEFAULT_TYPE = "jks";
    private static final int DEFAULT_REFRESH_SECS = 300;

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
                environment.optionalInt(truststorePrefix + SUFFIX_REFRESH_SECS).orElse(DEFAULT_REFRESH_SECS) * 1000
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
                environment.optionalInt(keystorePrefix + SUFFIX_REFRESH_SECS).orElse(DEFAULT_REFRESH_SECS) * 1000
        ));
    }
}
