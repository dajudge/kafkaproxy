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
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.Map;

import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static org.testcontainers.utility.MountableFile.forHostPath;

public class KafkaSslConfiguration implements SslConfiguration {
    private static final String KAFKA_SECRETS_DIR = "/etc/kafka/secrets/";
    private static final String KEYSTORE_PASSWORD_FILENAME = "keystore.pwd";
    private static final String KEY_PASSWORD_FILENAME = "key.pwd";
    private static final String KEYSTORE_FILENAME = "keystore.jks";
    private static final String TRUSTSTORE_LOCATION = KAFKA_SECRETS_DIR + "truststore.jks";
    private static final String KEYSTORE_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_FILENAME;
    private static final String KEYSTORE_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_PASSWORD_FILENAME;
    private static final String KEY_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEY_PASSWORD_FILENAME;

    private final TemporaryFolder tempDir;
    private final Collection<String> brokers;

    public KafkaSslConfiguration(final TemporaryFolder tempDir, final Collection<String> brokers) {
        this.tempDir = tempDir;
        this.brokers = brokers;
    }

    @Override
    public SslTestSetup createSslSetup() {
        return sslSetup("CN=KafkaCA", tempDir.getRoot())
                .withBrokers(brokers)
                .build();
    }

    @Override
    public TestEnvironment applyProxyConfig(
            final TestEnvironment environment,
            final SslTestSetup sslSetup
    ) {
        final SslTestAuthority kafkaAuthority = sslSetup.getAuthority();
        return environment.withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", "kafka/truststore.jks")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", kafkaAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true")
                .withFile("kafka/truststore.jks", kafkaAuthority.getTrustStore());
    }

    @Override
    public void applyClientConfig(final Map<String, Object> config, final SslTestSetup sslTestSetup) {
    }

    @Override
    public <T extends GenericContainer<T>> GenericContainer<T> applyKafkaConfig(
            final String brokerName,
            final GenericContainer<T> container,
            final SslTestSetup sslSetup
    ) {
        final SslTestAuthority ca = sslSetup.getAuthority();
        final SslTestKeystore keystore = sslSetup.getBroker(brokerName);
        return container.withCopyFileToContainer(
                forHostPath(ca.getTrustStore().getAbsolutePath()),
                TRUSTSTORE_LOCATION
        )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeyStore().getAbsolutePath()),
                        KEYSTORE_LOCATION
                )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeystorePasswordFile().getAbsolutePath()),
                        KEYSTORE_PASSWORD_LOCATION
                )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeyPasswordFile().getAbsolutePath()),
                        KEY_PASSWORD_LOCATION
                )
                .withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", TRUSTSTORE_LOCATION)
                .withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", ca.getTrustStorePassword())
                .withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KEYSTORE_FILENAME)
                .withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME)
                .withEnv("KAFKA_SSL_KEY_CREDENTIALS", KEY_PASSWORD_FILENAME)
                .withEnv("KAFKA_LISTENERS", "SSL://0.0.0.0:9092,PLAINTEXT://localhost:9093")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT");

    }
}
