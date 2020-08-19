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

package com.dajudge.kafkaproxy.roundtrip.client;

import com.dajudge.kafkaproxy.roundtrip.comm.ClientSecurity;
import com.dajudge.kafkaproxy.roundtrip.comm.ClientSslConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.dajudge.kafkaproxy.roundtrip.util.Util.indent;
import static com.dajudge.kafkaproxy.roundtrip.util.Util.safeToString;
import static java.util.stream.Collectors.joining;

public class ClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClientFactory.class);
    private final String bootstrapServers;
    private final ClientSecurity clientSecurity;

    public ClientFactory(
            final String bootstrapServers,
            final ClientSecurity clientSecurity
    ) {
        this.bootstrapServers = bootstrapServers;
        this.clientSecurity = clientSecurity;
    }

    private Map<String, Object> connectConfig(final ClientSslConfig sslConfig) {
        return new HashMap<String, Object>() {{
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, clientSecurity.getProtocol());
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslConfig.getKeyStoreLocation());
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, safeToString(sslConfig.getKeyStorePassword()));
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, sslConfig.getKeyStoreType());
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, safeToString(sslConfig.getKeyPassword()));
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientSecurity.getTrustStoreLocation());
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, safeToString(clientSecurity.getTrustStorePassword()));
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, clientSecurity.getTrustStoreType());
        }};
    }

    public AdminClient admin(final String dn) {
        final Map<String, Object> config = connectConfig(clientSecurity.newClient(dn));
        dumpConfig("Admin", config);
        return AdminClient.create(config);
    }

    public KafkaProducer<String, String> producer(final String dn) {
        final Map<String, Object> config = connectConfig(clientSecurity.newClient(dn));
        dumpConfig("Producer", config);
        return new KafkaProducer<>(config, new StringSerializer(), new StringSerializer());
    }

    public KafkaConsumer<String, String> consumer(final String dn, final String groupId) {
        final Map<String, Object> config = new HashMap<String, Object>(connectConfig(clientSecurity.newClient(dn))) {{
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        }};
        dumpConfig("Consumer", config);
        return new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer());
    }

    private void dumpConfig(final String type, final Map<String, Object> config) {
        LOG.info("{}:\n{}", type, indent(4, config.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(joining("\n"))));
    }
}
