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

package com.dajudge.kafkaproxy.util.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.reverse;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public final class KafkaClusterBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaClusterBuilder.class);

    private KafkaClusterBuilder() {

    }

    private static GenericContainer zk(final Network network, final int zkPort) {
        return new GenericContainer("confluentinc/cp-zookeeper:5.2.1")
                .withNetworkAliases("zk")
                .withNetwork(network)
                .withEnv("ZOOKEEPER_CLIENT_PORT", "" + zkPort)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*binding to port.*"));
    }

    private static GenericContainer kafkaContainer(
            final String hostname,
            final int brokerId,
            final Network network,
            final int zkPort,
            final BiFunction<String, Integer, String> advertisedListenersFunction
    ) {
        final GenericContainer container = new GenericContainer("confluentinc/cp-kafka:5.2.1");
        return container
                .withNetwork(network)
                .withNetworkAliases(hostname)
                .withCopyFileToContainer(forClasspathResource("customEntrypoint.sh", 777), "/customEntrypoint.sh")
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zk:" + zkPort)
                .withEnv("CONFLUENT_SUPPORT_METRICS_ENABLE", "0")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                .withEnv("KAFKA_BROKER_ID", "" + brokerId)
                .withEnv("KAFKA_NUM_PARTITIONS", "10")
                .withCommand("sh", "/customEntrypoint.sh")
                .withExposedPorts(9092)
                .waitingFor(new LogMessageWaitStrategy() {
                    @Override
                    public void waitUntilReady(final WaitStrategyTarget waitStrategyTarget) {
                        try {
                            final int mappedPort = container.getMappedPort(9092);
                            final String advertisedListeners = advertisedListenersFunction.apply(hostname, mappedPort);
                            LOG.info("Writing advertised listeners to container: {}", advertisedListeners);
                            final String containerFile = "/tmp/advertisedListeners.txt";
                            final String[] cmd = {"/bin/sh", "-c", "echo \"" + advertisedListeners + "\" > " + containerFile};
                            final int exitCode = container.execInContainer(cmd).getExitCode();
                            if (exitCode != 0) {
                                throw new IllegalStateException("Unexpected exit code: " + exitCode);
                            }
                        } catch (final Exception e) {
                            throw new RuntimeException("Failed to write advertosed listeners to container", e);
                        }
                        super.waitUntilReady(waitStrategyTarget);
                    }
                }.withRegEx(".*started \\(kafka.server.KafkaServer\\).*"));
    }

    public static KafkaCluster build(
            final Collection<String> brokers,
            final ContainerConfigurator configurator,
            final BiFunction<String, Integer, String> advertisedListeners
    ) {
        LOG.info("Building cluster with brokers {}...", brokers);
        final List<AutoCloseable> resources = new ArrayList<>();
        final int zkPort = 2181;
        final Network network = Network.newNetwork();
        resources.add(network);
        final GenericContainer zk = zk(network, zkPort);
        resources.add(zk);
        zk.start();
        final AtomicInteger brokerIdCounter = new AtomicInteger();
        final Map<String, Integer> portMap = new HashMap<>();
        brokers.forEach(broker -> {
            final int brokerId = brokerIdCounter.incrementAndGet();
            final GenericContainer container = kafkaContainer(broker, brokerId, network, zkPort, advertisedListeners);
            final GenericContainer kafka = configurator.configure(broker, container);
            kafka.start();
            resources.add(kafka);
            portMap.put(broker, kafka.getMappedPort(9092));
        });
        reverse(resources); // Ensure proper close order
        return new KafkaCluster(resources, portMap);
    }
}
