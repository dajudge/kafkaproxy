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

import com.dajudge.kafkaproxy.ProxyApplication;
import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.broker.BrokerMapParser;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator;
import com.dajudge.kafkaproxy.util.kafka.KafkaCluster;
import com.dajudge.kafkaproxy.util.kafka.KafkaClusterBuilder;
import com.dajudge.kafkaproxy.util.roundtrip.clientssl.ClientSslClientConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.clientssl.ClientSslEnvConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.kakfassl.KafkaSslContainerConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.kakfassl.KafkaSslEnvConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.dajudge.kafkaproxy.util.brokermap.BrokerMapBuilder.brokerMapFile;
import static com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator.composite;
import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static java.util.Collections.singletonList;

public class RoundtripTestBuilder {
    private Collection<AutoCloseable> resources = new ArrayList<>();
    private Collection<EnvConfigurator> envConfigurators = new ArrayList<>();
    private Collection<ContainerConfigurator> kafkaConfigurators = new ArrayList<>();
    private Collection<ClientConfigurator> clientConfigurators = new ArrayList<>();
    private int messagesToSend = 1;
    private int producerCount = 1;
    private int consumerCount = 1;
    private long testTimeout = 30000;
    private Collection<String> brokers;
    private String proxyHostname;
    private BiFunction<String, Integer, String> advertisedListeners;

    private RoundtripTestBuilder() {
    }

    public static RoundtripTestBuilder roundtripTest() {
        return new RoundtripTestBuilder();
    }

    public RoundtripTestBuilder withSslKafka(final Collection<String> brokers) {
        final TemporaryFolder tempDir = createTempFolder();
        final SslTestSetup sslSetup = sslSetup("CN=KafkaCA", tempDir.getRoot())
                .withBrokers(brokers)
                .build();
        resources.add(new TemporaryFolderResource(tempDir));
        envConfigurators.add(new KafkaSslEnvConfigurator(sslSetup));
        final KafkaSslContainerConfigurator e = new KafkaSslContainerConfigurator(sslSetup);
        kafkaConfigurators.add(e);
        advertisedListeners = e::advertisedListeners;
        this.brokers = brokers;
        return this;
    }

    public RoundtripTestBuilder withConsumerCount(final int consumerCount) {
        this.consumerCount = consumerCount;
        return this;
    }

    public RoundtripTestBuilder withProducerCount(final int producerCount) {
        this.producerCount = producerCount;
        return this;
    }

    public RoundtripTestBuilder withMessagesToSend(final int messagesToSend) {
        this.messagesToSend = messagesToSend;
        return this;
    }

    public RoundtripTestBuilder withTimeout(final int timeout) {
        this.testTimeout = timeout;
        return this;
    }

    public RoundtripTestBuilder withPlaintextKafka(final Collection<String> brokers) {
        this.brokers = brokers;
        return this;

    }

    public RoundtripTestBuilder withPlaintextClient(final String hostname) {
        this.proxyHostname = hostname;
        return this;
    }

    public RoundtripTestBuilder withSslClient(final String hostname) {
        final TemporaryFolder tempDir = createTempFolder();
        final SslTestSetup sslSetup = sslSetup("CN=ClientCA", tempDir.getRoot())
                .withBrokers(singletonList(hostname))
                .build();
        resources.add(new TemporaryFolderResource(tempDir));
        envConfigurators.add(new ClientSslEnvConfigurator(sslSetup, hostname));
        clientConfigurators.add(new ClientSslClientConfigurator(sslSetup));
        proxyHostname = hostname;
        return this;
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

    public RoundtripTest build() {
        final KafkaCluster kafkaCluster = KafkaClusterBuilder.build(
                brokers,
                composite(kafkaConfigurators),
                advertisedListeners
        );

        final byte[] brokerMapFile = brokerMapFile(kafkaCluster, proxyHostname);
        final BrokerMap brokermap = new BrokerMapParser(new ByteArrayInputStream(brokerMapFile)).getBrokerMap();
        final BrokerMapping firstBroker = brokermap.getAll().get(0);
        final String bootstrapServers = firstBroker.getProxy().getHost() + ":" + firstBroker.getProxy().getPort();
        TestEnvironment env = new TestEnvironment()
                .withFile("/etc/kafkaproxy/brokermap.yml", brokerMapFile);
        for (final EnvConfigurator envConfigurator : envConfigurators) {
            env = envConfigurator.configure(env);
        }
        final ProxyApplication proxyApp = ProxyApplication.create(env);

        final Map<String, Object> baseProps = new HashMap<>();
        baseProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        clientConfigurators.forEach(it -> it.apply(baseProps));

        final Map<String, Object> producerConfig = new HashMap<>(baseProps);
        producerConfig.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);

        final Map<String, Object> consumerConfig = new HashMap<>(baseProps);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "testgroup");
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final RoundtripTester tester = new RoundtripTester(
                producerConfig,
                consumerConfig,
                messagesToSend,
                producerCount,
                consumerCount
        );

        final RoundtripCounter roundtrip = new RoundtripCounter(testTimeout, messagesToSend);

        proxyApp.start();

        return new RoundtripTest(proxyApp, tester, roundtrip, resources);
    }

}
