/*
 * Copyright 2019-2020 Alex Stockinger
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

import com.dajudge.kafkaproxy.ProxyChannelFactory.BrokerMappingStrategy;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping.Endpoint;
import com.dajudge.kafkaproxy.config.broker.BrokerConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toSet;

public class FixedBootstrapMappingStrategy implements BrokerMappingStrategy {
    private final Map<String, BrokerMapping> allMappings = new HashMap<>();
    private final Collection<BrokerMapping> bootstrapMappings;
    private final String proxyHostname;
    private int nextBrokerPort;

    public FixedBootstrapMappingStrategy(final BrokerConfig brokerConfig) {
        nextBrokerPort = brokerConfig.getProxyBasePort();
        proxyHostname = brokerConfig.getProxyHostname();
        bootstrapMappings = brokerConfig.getBootstrapBrokers().stream()
                .map(this::getBrokerMapping)
                .collect(toSet());
    }

    @Override
    public BrokerMapping getBrokerMapping(final Endpoint brokerEndpoint) {
        return allMappings.computeIfAbsent(keyOf(brokerEndpoint), key -> new BrokerMapping(
                brokerEndpoint,
                new Endpoint(proxyHostname, nextBrokerPort++)
        ));
    }

    @Override
    public Collection<BrokerMapping> getBootstrapBrokers() {
        return bootstrapMappings;
    }

    private String keyOf(final Endpoint host) {
        return host.getHost() + host.getPort();
    }
}
