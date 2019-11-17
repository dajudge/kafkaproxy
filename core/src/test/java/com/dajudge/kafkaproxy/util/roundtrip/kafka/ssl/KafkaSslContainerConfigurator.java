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

package com.dajudge.kafkaproxy.util.roundtrip.kafka.ssl;

import com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.testcontainers.containers.GenericContainer;

import static org.testcontainers.utility.MountableFile.forHostPath;

public class KafkaSslContainerConfigurator implements ContainerConfigurator {
    private static final String KAFKA_SECRETS_DIR = "/etc/kafka/secrets/";
    private static final String KEYSTORE_PASSWORD_FILENAME = "keystore.pwd";
    private static final String KEY_PASSWORD_FILENAME = "key.pwd";
    private static final String KEYSTORE_FILENAME = "keystore.jks";
    private static final String TRUSTSTORE_LOCATION = KAFKA_SECRETS_DIR + "truststore.jks";
    private static final String KEYSTORE_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_FILENAME;
    private static final String KEYSTORE_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_PASSWORD_FILENAME;
    private static final String KEY_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEY_PASSWORD_FILENAME;
    private final SslTestSetup sslSetup;

    public KafkaSslContainerConfigurator(final SslTestSetup sslSetup) {
        this.sslSetup = sslSetup;
    }

    @Override
    public GenericContainer configure(final String hostname, final GenericContainer c) {
        final SslTestAuthority ca = sslSetup.getAuthority();
        final SslTestKeystore keystore = sslSetup.getBroker(hostname);
        return c.withCopyFileToContainer(
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

    public String advertisedListeners(final String hostname, final Integer port) {
        return "SSL://" + hostname + ":" + port + ",PLAINTEXT://localhost:9093";
    }
}
