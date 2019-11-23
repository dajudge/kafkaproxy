package com.dajudge.kafkaproxy.util.roundtrip.kafka.twowayssl;

import com.dajudge.kafkaproxy.util.kafka.ContainerConfigurator;
import org.testcontainers.containers.GenericContainer;

public class KafkaTwoWaySslContainerConfigurator implements ContainerConfigurator {
    @Override
    public GenericContainer configure(final String hostname, final GenericContainer c) {
        return c.withEnv("KAFKA_SSL_CLIENT_AUTH", "required");
    }
}
