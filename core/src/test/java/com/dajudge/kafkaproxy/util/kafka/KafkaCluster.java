package com.dajudge.kafkaproxy.util.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class KafkaCluster implements AutoCloseable {
    private static Logger LOG = LoggerFactory.getLogger(KafkaCluster.class);
    private final List<AutoCloseable> resources;
    private final Map<String, Integer> brokers;

    /**
     * Constructor.
     *
     * @param resources the resources to close on shutdown. Guaranteed to be destroyed in order
     *                  of list.
     * @param brokers   map from broker name to port of broker.
     */
    KafkaCluster(
            final List<AutoCloseable> resources,
            final Map<String, Integer> brokers
    ) {
        this.resources = resources;
        this.brokers = unmodifiableMap(brokers);
    }

    public Map<String, Integer> getBrokers() {
        return brokers;
    }

    @Override
    public void close() {
        // Order is relevant
        resources.forEach(container -> {
            try {
                container.close();
            } catch (final Exception e) {
                LOG.error("Failed to cleanly shutdown container", e);
            }
        });
    }
}
