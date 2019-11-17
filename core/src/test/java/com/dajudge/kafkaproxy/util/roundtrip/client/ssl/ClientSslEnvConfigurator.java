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

package com.dajudge.kafkaproxy.util.roundtrip.client.ssl;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.roundtrip.EnvConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;

public class ClientSslEnvConfigurator implements EnvConfigurator {
    private final SslTestSetup sslSetup;
    private final String hostname;

    public ClientSslEnvConfigurator(final SslTestSetup sslSetup, final String hostname) {
        this.sslSetup = sslSetup;
        this.hostname = hostname;
    }

    @Override
    public TestEnvironment configure(final TestEnvironment e) {
        final SslTestAuthority clientAuthority = sslSetup.getAuthority();
        final SslTestKeystore clientBroker = sslSetup.getBroker(hostname);
        return e.withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", "client/truststore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", clientAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", "client/keystore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", clientBroker.getKeystorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", clientBroker.getKeyPassword())
                .withFile("client/truststore.jks", clientAuthority.getTrustStore())
                .withFile("client/keystore.jks", clientBroker.getKeyStore());
    }
}
