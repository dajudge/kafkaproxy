/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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

package com.dajudge.kafkaproxy.roundtrip.cluster.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import static java.lang.String.valueOf;

public class ZookeeperContainer extends GenericContainer<ZookeeperContainer> {
    private static final String NETWORK_ALIAS = "zookeeper";
    private static final int ZOOKEEPER_PORT = 2181;

    public ZookeeperContainer(final Network network) {
        super("confluentinc/cp-zookeeper:5.4.1");
        this.withNetworkAliases(NETWORK_ALIAS)
                .withNetwork(network)
                .withEnv("ZOOKEEPER_CLIENT_PORT", valueOf(ZOOKEEPER_PORT))
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*binding to port.*"));
    }

    public String getEndpoint() {
        return NETWORK_ALIAS + ":" + ZOOKEEPER_PORT;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
