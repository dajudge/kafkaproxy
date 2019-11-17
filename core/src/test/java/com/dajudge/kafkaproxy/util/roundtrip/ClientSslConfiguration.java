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

package com.dajudge.kafkaproxy.util.roundtrip;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static java.util.Collections.singletonList;

public class ClientSslConfiguration implements SslConfiguration {
    private final TemporaryFolder tempDir;
    private final String hostname;

    public ClientSslConfiguration(final TemporaryFolder tempDir, final String hostname) {
        this.tempDir = tempDir;
        this.hostname = hostname;
    }

    @Override
    public SslTestSetup createSslSetup() {
        return sslSetup("CN=ClientCA", tempDir.getRoot())
                .withBrokers(singletonList(hostname))
                .build();
    }

    @Override
    public TestEnvironment applyProxyConfig(
            final TestEnvironment environment,
            final SslTestSetup sslSetup
    ) {
        final SslTestAuthority clientAuthority = sslSetup.getAuthority();
        final SslTestKeystore clientBroker = sslSetup.getBroker(hostname);
        return environment.withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", "client/truststore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", clientAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", "client/keystore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", clientBroker.getKeystorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", clientBroker.getKeyPassword())
                .withFile("client/truststore.jks", clientAuthority.getTrustStore())
                .withFile("client/keystore.jks", clientBroker.getKeyStore());
    }

    @Override
    public void applyClientConfig(
            final Map<String, Object> config,
            final SslTestSetup sslSetup
    ) {
        final SslTestAuthority clientAuthority = sslSetup.getAuthority();
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientAuthority.getTrustStore().getAbsolutePath());
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, clientAuthority.getTrustStorePassword());
    }

    @Override
    public <T extends GenericContainer<T>> GenericContainer<T> applyKafkaConfig(
            final String brokerName,
            final GenericContainer<T> container,
            final SslTestSetup sslSetup
    ) {
        return container;
    }
}
