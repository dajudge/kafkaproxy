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

package com.dajudge.kafkaproxy.config;


import com.dajudge.proxybase.config.Endpoint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.Integer.parseUnsignedInt;
import static java.util.stream.Collectors.toList;

public class BrokerConfigSource implements ConfigSource<BrokerConfigSource.BrokerConfig> {

    @Override
    public Class<BrokerConfig> getConfigClass() {
        return BrokerConfig.class;
    }

    @Override
    public Optional<BrokerConfig> parse(final Environment environment) {
        return Optional.of(new BrokerConfig(
                getBootstrapBrokers(environment),
                environment.requiredString("KAFKAPROXY_HOSTNAME"),
                environment.requiredInt("KAFKAPROXY_BASE_PORT"),
                environment.requiredString("KAFKAPROXY_BIND_ADDRESS", "0.0.0.0")
        ));
    }

    private List<Endpoint> getBootstrapBrokers(final Environment environment) {
        final String bootstrapServers = environment.requiredString("KAFKAPROXY_BOOTSTRAP_SERVERS");
        return Stream.of(bootstrapServers.split(","))
                .map(bootstrapServer -> {
                    final String[] bootstrapServerParts = bootstrapServer.split(":");
                    return new Endpoint(bootstrapServerParts[0], parseUnsignedInt(bootstrapServerParts[1]));
                })
                .collect(toList());

    }

    public static class BrokerConfig {
        private final List<Endpoint> bootstrapBrokers;
        private final String proxyHostname;
        private final int proxyBasePort;
        private final String bindAddress;

        public BrokerConfig(
                final List<Endpoint> bootstrapBrokers,
                final String proxyHostname,
                final int proxyBasePort,
                final String bindAddress
        ) {
            this.bootstrapBrokers = bootstrapBrokers;
            this.proxyHostname = proxyHostname;
            this.proxyBasePort = proxyBasePort;
            this.bindAddress = bindAddress;
        }

        public List<Endpoint> getBootstrapBrokers() {
            return bootstrapBrokers;
        }

        public String getProxyHostname() {
            return proxyHostname;
        }

        public int getProxyBasePort() {
            return proxyBasePort;
        }

        public String getBindAddress() {
            return bindAddress;
        }
    }
}
