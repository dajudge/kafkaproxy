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

import com.dajudge.proxybase.config.UpstreamSslConfig;

import java.util.Optional;

import static com.dajudge.kafkaproxy.config.KeyStoreConfigHelper.requiredKeyStoreConfig;


public class UpstreamSslConfigSource implements ConfigSource<UpstreamSslConfig> {
    private static final String PREFIX_CLIENT_SSL = PREFIX + "CLIENT_SSL_";
    private static final String PROP_CLIENT_SSL_ENABLED = PREFIX_CLIENT_SSL + "ENABLED";
    private static final String PROP_CLIENT_SSL_AUTH_REQUIRED = PREFIX_CLIENT_SSL + "AUTH_REQUIRED";

    private static final boolean DEFAULT_CLIENT_SSL_ENABLED = false;
    private static final boolean DEFAULT_CLIENT_AUTH_REQUIRED = false;

    @Override
    public Class<UpstreamSslConfig> getConfigClass() {
        return UpstreamSslConfig.class;
    }

    @Override
    public Optional<UpstreamSslConfig> parse(final Environment environment) {
        if (!environment.requiredBoolean(PROP_CLIENT_SSL_ENABLED, DEFAULT_CLIENT_SSL_ENABLED)) {
            return Optional.empty();
        }
        return Optional.of(new UpstreamSslConfig(
                KeyStoreConfigHelper.loadTrustStoreConfig(environment, PREFIX_CLIENT_SSL),
                requiredKeyStoreConfig(environment, PREFIX_CLIENT_SSL),
                environment.requiredBoolean(PROP_CLIENT_SSL_AUTH_REQUIRED, DEFAULT_CLIENT_AUTH_REQUIRED)
        ));
    }

}
