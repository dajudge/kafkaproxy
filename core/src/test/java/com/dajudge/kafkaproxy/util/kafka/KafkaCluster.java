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

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class KafkaCluster implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaCluster.class);
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
