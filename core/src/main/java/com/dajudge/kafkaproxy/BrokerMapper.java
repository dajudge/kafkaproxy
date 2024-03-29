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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.config.BrokerConfigSource;
import com.dajudge.proxybase.config.Endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrokerMapper {
    private final Map<String, BrokerMapping> allMappings = new HashMap<>();
    private final String proxyHostname;
    private final List<Endpoint> bootstrapBrokers;
    private int nextBrokerPort;

    public BrokerMapper(final BrokerConfigSource.BrokerConfig brokerConfig) {
        nextBrokerPort = brokerConfig.getProxyBasePort();
        proxyHostname = brokerConfig.getProxyHostname();
        bootstrapBrokers = brokerConfig.getBootstrapBrokers();
    }

    public synchronized BrokerMapping getBrokerMapping(final Endpoint brokerEndpoint) {
        return allMappings.computeIfAbsent(keyOf(brokerEndpoint), key -> new BrokerMapping(
                brokerEndpoint,
                new Endpoint(proxyHostname, nextBrokerPort++)
        ));
    }

    public List<Endpoint> getBootstrapBrokers() {
        return bootstrapBrokers;
    }

    private String keyOf(final Endpoint host) {
        return host.getHost() + host.getPort();
    }
}
