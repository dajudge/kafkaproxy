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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.reverse;

public abstract class LolcatsKafkaBuilder extends KafkaClusterBuilder<LolcatsKafkaBuilder, KafkaCluster> {
    private static final Logger LOG = LoggerFactory.getLogger(LolcatsKafkaBuilder.class);

    public KafkaCluster build() {
        LOG.info("Building cluster with brokers {}...", brokers);
        final List<AutoCloseable> resources = new ArrayList<>();
        final int zkPort = 2181;
        final Network network = Network.newNetwork();
        resources.add(network);
        final GenericContainer zk = zk(network, zkPort);
        resources.add(zk);
        zk.start();
        final AtomicInteger brokerId = new AtomicInteger();
        final Map<String, Integer> portMap = new HashMap<>();
        brokers.forEach(broker -> {
            final GenericContainer kafka = kafkaSsl(
                    network,
                    broker,
                    brokerId.incrementAndGet(),
                    zkPort
            );
            kafka.start();
            resources.add(kafka);
            portMap.put(broker, kafka.getMappedPort(9092));
        });
        reverse(resources); // Ensure proper close order
        return new KafkaCluster(resources, portMap);
    }

    private GenericContainer kafkaSsl(
            final Network network,
            final String hostname,
            final int brokerId,
            final int zkPort
    ) {
        return withSsl(hostname, kafkaContainer(hostname, brokerId, network, zkPort));
    }

    protected abstract GenericContainer withSsl(final String brokerName, final GenericContainer kafkaContainer);

    @Override
    protected String advertisedListeners(final String hostname, final int port) {
        return "SSL://" + hostname + ":" + port + ",PLAINTEXT://localhost:9093";
    }
}
