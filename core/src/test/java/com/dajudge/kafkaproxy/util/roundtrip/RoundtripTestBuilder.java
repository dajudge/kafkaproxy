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
import com.dajudge.kafkaproxy.util.brokermap.TestBrokerMap;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator;
import com.dajudge.kafkaproxy.util.kafka.KafkaCluster;
import com.dajudge.kafkaproxy.util.kafka.KafkaClusterBuilder;
import com.dajudge.kafkaproxy.util.roundtrip.client.ssl.ClientSslClientConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.client.ssl.ClientSslEnvConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.kafka.plaintext.KafkaPlaintextConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.kafka.ssl.KafkaSslContainerConfigurator;
import com.dajudge.kafkaproxy.util.roundtrip.kafka.ssl.KafkaSslEnvConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.dajudge.kafkaproxy.util.brokermap.BrokerMapBuilder.brokerMapFor;
import static com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator.composite;
import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static java.util.Collections.singletonList;

public class RoundtripTestBuilder {
    private Collection<AutoCloseable> resources = new ArrayList<>();
    private Collection<EnvConfigurator> envConfigurators = new ArrayList<>();
    private Collection<ContainerConfigurator> kafkaConfigurators = new ArrayList<>();
    private Collection<ClientConfigurator> baseClientConfigurators = new ArrayList<>();
    private Collection<ClientConfigurator> consumerClientConfigurators = new ArrayList<>(
            singletonList(defaultConsumerClientConfig())
    );
    private Collection<ClientConfigurator> producerClientConfigurators = new ArrayList<>(
            singletonList(defaultProducerClientConfig())
    );
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
        final SslTestSetup sslSetup = sslSetup("CN=KafkaCA", newTempFolder().getRoot())
                .withBrokers(brokers)
                .build();
        envConfigurators.add(new KafkaSslEnvConfigurator(sslSetup));
        final KafkaSslContainerConfigurator kafkaConfigurator = new KafkaSslContainerConfigurator(sslSetup);
        kafkaConfigurators.add(kafkaConfigurator);
        advertisedListeners = kafkaConfigurator::advertisedListeners;
        this.brokers = brokers;
        return this;
    }

    public RoundtripTestBuilder withPlaintextKafka(final Collection<String> brokers) {
        this.brokers = brokers;
        final KafkaPlaintextConfigurator kafkaConfigurator = new KafkaPlaintextConfigurator();
        kafkaConfigurators.add(kafkaConfigurator);
        advertisedListeners = kafkaConfigurator::advertisedListeners;
        return this;
    }

    public RoundtripTestBuilder withPlaintextClient(final String hostname) {
        this.proxyHostname = hostname;
        return this;
    }

    public RoundtripTestBuilder withSslClient(final String hostname) {
        final SslTestSetup sslSetup = sslSetup("CN=ClientCA", newTempFolder().getRoot())
                .withBrokers(singletonList(hostname))
                .build();
        envConfigurators.add(new ClientSslEnvConfigurator(sslSetup, hostname));
        baseClientConfigurators.add(new ClientSslClientConfigurator(sslSetup));
        proxyHostname = hostname;
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

        final TestBrokerMap brokerMap = brokerMapFor(kafkaCluster, proxyHostname);
        final TestEnvironment env = new TestEnvironment()
                .withFile("/etc/kafkaproxy/brokermap.yml", brokerMap.getData());
        for (final EnvConfigurator envConfigurator : envConfigurators) {
            envConfigurator.configure(env);
        }

        baseClientConfigurators.add(defaultBaseClientConfig(brokerMap));

        return new RoundtripTest(
                ProxyApplication.create(env).start(),
                new RoundtripTester(
                        producerConfig(),
                        consumerConfig(),
                        messagesToSend,
                        producerCount,
                        consumerCount
                ),
                new RoundtripCounter(testTimeout, messagesToSend),
                resources
        );
    }

    @NotNull
    private Map<String, Object> producerConfig() {
        final Map<String, Object> producerConfig = new HashMap<>(getBaseConfig());
        producerClientConfigurators.forEach(it -> it.apply(producerConfig));
        return producerConfig;
    }

    @NotNull
    private Map<String, Object> consumerConfig() {
        final Map<String, Object> consumerConfig = new HashMap<>(getBaseConfig());
        consumerClientConfigurators.forEach(it -> it.apply(consumerConfig));
        return consumerConfig;
    }

    @NotNull
    private Map<String, Object> getBaseConfig() {
        final Map<String, Object> baseProps = new HashMap<>();
        baseClientConfigurators.forEach(it -> it.apply(baseProps));
        return baseProps;
    }


    private ClientConfigurator defaultProducerClientConfig() {
        return config -> {
            config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        };
    }

    private ClientConfigurator defaultConsumerClientConfig() {
        return config -> {
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "testgroup");
            config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        };
    }

    @NotNull
    private ClientConfigurator defaultBaseClientConfig(final TestBrokerMap brokerMap) {
        return config -> config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerMap.getBootstrapServers());
    }

    private TemporaryFolder newTempFolder() {
        final TemporaryFolder tempDir = createTempFolder();
        resources.add(new TemporaryFolderResource(tempDir));
        return tempDir;
    }
}
