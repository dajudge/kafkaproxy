/*
 * Copyright 2019 Alex Stockinger
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

package com.dajudge.kafkaproxy.config.proxyssl;

import com.dajudge.kafkaproxy.config.ConfigSource;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;

public class ProxySslConfigSource implements ConfigSource<ProxySslConfig> {
    private static final String PREFIX_CLIENT_SSL = PREFIX + "CLIENT_SSL_";
    private static final String PROP_CLIENT_SSL_ENABLED = PREFIX_CLIENT_SSL + "ENABLED";
    private static final String PROP_CLIENT_SSL_TRUSTSTORE_LOCATION = PREFIX_CLIENT_SSL + "TRUSTSTORE_LOCATION";
    private static final String PROP_CLIENT_SSL_TRUSTSTORE_PASSWORD = PREFIX_CLIENT_SSL + "TRUSTSTORE_PASSWORD";
    private static final String PROP_CLIENT_SSL_KEYSTORE_LOCATION = PREFIX_CLIENT_SSL + "KEYSTORE_LOCATION";
    private static final String PROP_CLIENT_SSL_KEYSTORE_PASSWORD = PREFIX_CLIENT_SSL + "KEYSTORE_PASSWORD";
    private static final String PROP_CLIENT_SSL_KEY_PASSWORD = PREFIX_CLIENT_SSL + "KEY_PASSWORD";
    private static final boolean DEFAULT_CLIENT_SSL_ENABLED = false;

    @Override
    public Class<ProxySslConfig> getConfigClass() {
        return ProxySslConfig.class;
    }

    @Override
    public ProxySslConfig parse(final Environment environment) {
        final boolean enabled = environment.requiredBoolean(PROP_CLIENT_SSL_ENABLED, DEFAULT_CLIENT_SSL_ENABLED);
        if (!enabled) {
            return ProxySslConfig.DISABLED;
        }
        return new ProxySslConfig(
                true,
                environment.optionalFile(PROP_CLIENT_SSL_TRUSTSTORE_LOCATION).orElse(null),
                environment.optionalString(PROP_CLIENT_SSL_TRUSTSTORE_PASSWORD).orElse(null),
                environment.optionalFile(PROP_CLIENT_SSL_KEYSTORE_LOCATION).orElse(null),
                environment.optionalString(PROP_CLIENT_SSL_KEYSTORE_PASSWORD).orElse(null),
                environment.optionalString(PROP_CLIENT_SSL_KEY_PASSWORD).orElse(null)
        );
    }
}
