package com.dajudge.kafkaproxy.util.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.testcontainers.utility.MountableFile.forClasspathResource;

public abstract class KafkaClusterBuilder<B extends KafkaClusterBuilder, T extends KafkaCluster> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaClusterBuilder.class);
    protected final Set<String> brokers = new HashSet<>();

    protected KafkaClusterBuilder() {
    }

    public T build() {
        throw new UnsupportedOperationException();
    }

    public B withBrokers(final Set<String> name) {
        brokers.addAll(name);
        return (B) this;
    }

    public B withBroker(final String name) {
        brokers.add(name);
        return (B) this;
    }

    protected static GenericContainer zk(final Network network, final int zkPort) {
        return new GenericContainer("confluentinc/cp-zookeeper:5.2.1")
                .withNetworkAliases("zk")
                .withNetwork(network)
                .withEnv("ZOOKEEPER_CLIENT_PORT", "" + zkPort)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*binding to port.*"));
    }

    protected GenericContainer kafkaContainer(final String hostname, final int brokerId, final Network network, final int zkPort) {
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
                            final String advertisedListeners = advertisedListeners(hostname, mappedPort);
                            LOG.info("Writing advertised listeners to container: {}", advertisedListeners);
                            container.execInContainer("/bin/sh", "-c", "echo \"" + advertisedListeners + "\" > /tmp/advertisedListeners.txt").getExitCode();
                        } catch (final IOException | InterruptedException e) {
                            throw new RuntimeException("Failed to write external broker port to container", e);
                        }
                        super.waitUntilReady(waitStrategyTarget);
                    }
                }.withRegEx(".*started \\(kafka.server.KafkaServer\\).*"));
    }

    protected abstract String advertisedListeners(String hostname, int port);
}
