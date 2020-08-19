/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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


import com.dajudge.proxybase.config.DownstreamSslConfig;

import java.util.Optional;

import static com.dajudge.kafkaproxy.config.KeyStoreConfigHelper.*;

public class DownstreamSslConfigSource implements ConfigSource<DownstreamSslConfig> {
    private static final String KAFKA_SSL_PREFIX = PREFIX + "KAFKA_SSL_";
    private static final String ENV_KAFKA_SSL_ENABLED = KAFKA_SSL_PREFIX + "ENABLED";
    private static final String ENV_KAFKA_SSL_VERIFY_HOSTNAME = KAFKA_SSL_PREFIX + "VERIFY_HOSTNAME";
    private static final boolean DEFAULT_KAFKA_SSL_ENABLED = false;
    private static final boolean DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME = true;

    @Override
    public Class<DownstreamSslConfig> getConfigClass() {
        return DownstreamSslConfig.class;
    }

    @Override
    public Optional<DownstreamSslConfig> parse(final Environment environment) {
        final boolean enabled = environment.requiredBoolean(ENV_KAFKA_SSL_ENABLED, DEFAULT_KAFKA_SSL_ENABLED);
        if (!enabled) {
            return Optional.empty();
        }
        final DownstreamSslConfig downstreamConfig = new DownstreamSslConfig(
                requiredTrustStoreConfig(environment, KAFKA_SSL_PREFIX),
                optionalKeyStoreConfig(environment, KAFKA_SSL_PREFIX),
                environment.requiredBoolean(ENV_KAFKA_SSL_VERIFY_HOSTNAME, DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME)
        );
        return Optional.of(downstreamConfig);
    }
}
