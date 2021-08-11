/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.reverse;

public class KafkaCluster implements AutoCloseable {
    private final List<GenericContainer<?>> containers;
    private final String bootstrapServers;

    KafkaCluster(
            final List<GenericContainer<?>> containers,
            final String bootstrapServers
    ) {
        this.containers = new ArrayList<>(containers);
        reverse(this.containers);
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public void close() {
        containers.forEach(Startable::close);
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public Set<String> getBootstrapServerList() {
        return new HashSet<>(asList(bootstrapServers.split(",")));
    }
}
