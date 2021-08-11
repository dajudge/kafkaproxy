package com.dajudge.kafkaproxy.itest.util;

import org.testcontainers.containers.GenericContainer;

public class KafkaProxyContainer extends GenericContainer<KafkaProxyContainer> {
    public KafkaProxyContainer() {
        super("localhost/kafkaproxy/kafkaproxy:latest");
        this.withNetworkMode("host");
    }
}
