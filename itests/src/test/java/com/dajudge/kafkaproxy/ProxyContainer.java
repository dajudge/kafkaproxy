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

package com.dajudge.kafkaproxy;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class ProxyContainer extends GenericContainer<ProxyContainer> {
    public static final int PROXY_BASE_PORT = 40000;

    public ProxyContainer(
            final String tag,
            final Network network,
            final String hostname,
            final String bootstrapServers
    ) {
        super("dajudge/kafkaproxy:" + tag);
        this.withNetwork(network)
                .withEnv("KAFKAPROXY_HOSTNAME", hostname)
                .withEnv("KAFKAPROXY_BASE_PORT", String.format("%d", PROXY_BASE_PORT))
                .withEnv("KAFKAPROXY_BOOTSTRAP_SERVERS", bootstrapServers)
                .withExposedPorts(8080)
                .withNetworkAliases(hostname);
    }
}
