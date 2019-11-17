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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.broker.BrokerMapParser;
import com.dajudge.kafkaproxy.roundtrip.RoundtripCounter;
import com.dajudge.kafkaproxy.roundtrip.RoundtripTester;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.kafka.KafkaClusterWihtSsl;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.dajudge.kafkaproxy.util.brokermap.BrokerMapBuilder.brokerMapFile;
import static com.dajudge.kafkaproxy.util.kafka.KafkaClusterWithSslBuilder.kafkaClusterWithSsl;
import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

public class RoundtripTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoundtripTest.class);
    @ClassRule
    public static final TemporaryFolder KAFKA_SSL_TEMP_DIR = createTempFolder();
    @ClassRule
    public static final TemporaryFolder CLIENT_SSL_TEMP_DIR = createTempFolder();

    private static final String BROKER_HOSTNAME = "localhost";
    private static final String PROXY_HOSTNAME = "localhost";
    private static final int MESSAGES_TO_SEND = 1;
    private static final int TEST_TIMEOUT = 2 * 60 * 1000;
    private static final int PRODUCERS = 1;
    private static final int CONSUMERS = 1;

    private final SslTestSetup clientSslSetup = sslSetup("CN=ClientCA", CLIENT_SSL_TEMP_DIR.getRoot())
            .withBrokers(singletonList(BROKER_HOSTNAME))
            .build();


    private ProxyApplication proxyApp;
    private String bootstrapServers;
    private KafkaClusterWihtSsl kafkaCluster;

    @Before
    public void setUp() {
        kafkaCluster = kafkaClusterWithSsl(sslSetup("CN=KafkaCA", KAFKA_SSL_TEMP_DIR.getRoot()))
                .withBroker(PROXY_HOSTNAME)
                .build();

        final byte[] brokerMapFile = brokerMapFile(kafkaCluster);
        final BrokerMap brokermap = new BrokerMapParser(new ByteArrayInputStream(brokerMapFile)).getBrokerMap();
        System.out.println(brokermap);
        final BrokerMapping firstBroker = brokermap.getAll().get(0);
        bootstrapServers = firstBroker.getProxy().getHost() + ":" + firstBroker.getProxy().getPort();

        final SslTestAuthority clientAuthority = clientSslSetup.getAuthority();
        final SslTestKeystore clientBroker = clientSslSetup.getBroker(PROXY_HOSTNAME);
        final SslTestAuthority kafkaAuthority = kafkaCluster.getAuthority();
        final Environment env = new TestEnvironment()
                .withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", "client/truststore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", clientAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", "client/keystore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", clientBroker.getKeystorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", clientBroker.getKeyPassword())
                .withFile("client/truststore.jks", clientAuthority.getTrustStore())
                .withFile("client/keystore.jks", clientBroker.getKeyStore())
                .withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", "kafka/truststore.jks")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", kafkaAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true")
                .withFile("kafka/truststore.jks", kafkaAuthority.getTrustStore())
                .withFile("/etc/kafkaproxy/brokermap.yml", brokerMapFile);

        proxyApp = ProxyApplication.create(env);
        proxyApp.start();
    }

    @After
    public void tearDown() {
        proxyApp.shutdown();
        kafkaCluster.close();
    }

    @Test
    public void test_roundtrip() {
        final Map<String, Object> baseProps = new HashMap<>();
        baseProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        baseProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        baseProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientSslSetup.getAuthority().getTrustStore().getAbsolutePath());
        baseProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, clientSslSetup.getAuthority().getTrustStorePassword());

        final Map<String, Object> producerProps = new HashMap<>(baseProps);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        final Map<String, Object> consumerProps = new HashMap<>(baseProps);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testgroup");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final RoundtripTester tester = new RoundtripTester(
                producerProps,
                consumerProps,
                MESSAGES_TO_SEND,
                PRODUCERS,
                CONSUMERS
        );
        final RoundtripCounter roundtrip = new RoundtripCounter(TEST_TIMEOUT, MESSAGES_TO_SEND);
        final long start = System.currentTimeMillis();
        tester.run(roundtrip);
        final long end = System.currentTimeMillis();
        assertTrue("Did not complete roundtrip", roundtrip.completed());
        LOG.info("Executed {} message roundtrip in {}ms", MESSAGES_TO_SEND, end - start);
    }


    private static TemporaryFolder createTempFolder() {
        final TemporaryFolder temporaryFolder = new TemporaryFolder();
        try {
            temporaryFolder.create();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return temporaryFolder;
    }
}
