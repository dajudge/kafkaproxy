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

package com.dajudge.kafkaproxy.brokermap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class BrokerMap {
    private final Map<String, BrokerMapping> byBrokerEndpoint;
    private final Map<String, BrokerMapping> byProxyName;

    public BrokerMap(final Collection<BrokerMapping> mappings) {
        this.byBrokerEndpoint = mappings.stream().collect(toMap(
                it -> it.getBroker().getHost() + ":" + it.getBroker().getPort(),
                it -> it
        ));
        this.byProxyName = mappings.stream().collect(toMap(
                BrokerMapping::getName,
                it -> it
        ));
    }

    public BrokerMapping getByBrokerEndpoint(final String host, final int port) {
        return byBrokerEndpoint.get(host + ":" + port);
    }

    public BrokerMapping getByProxyName(final String name) {
        return byProxyName.get(name);
    }

    @Override
    public String toString() {
        return "BrokerMap{" +
                "mappings=" + byBrokerEndpoint +
                '}';
    }

    public List<BrokerMapping> getAll() {
        return new ArrayList<>(byProxyName.values());
    }
}
