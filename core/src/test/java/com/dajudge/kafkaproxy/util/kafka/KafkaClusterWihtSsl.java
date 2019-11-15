package com.dajudge.kafkaproxy.util.kafka;

import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;

import java.util.List;
import java.util.Map;

public class KafkaClusterWihtSsl extends KafkaCluster {
    private final SslTestSetup sslSetup;

    /**
     * Constructor.
     *
     * @param resources the resources to close on shutdown. Guaranteed to be destroyed in order
     *                  of list.
     * @param brokers   map from broker name to port of broker.
     * @param sslSetup
     */
    KafkaClusterWihtSsl(
            final List<AutoCloseable> resources,
            final Map<String, Integer> brokers,
            final SslTestSetup sslSetup
    ) {
        super(resources, brokers);
        this.sslSetup = sslSetup;
    }

    public SslTestAuthority getAuthority() {
        return sslSetup.getAuthority();
    }
}
