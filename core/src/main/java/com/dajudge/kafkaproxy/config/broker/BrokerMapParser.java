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

package com.dajudge.kafkaproxy.config.broker;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BrokerMapParser {
    private final BrokerMap brokerMap;

    @SuppressWarnings("unchecked")
    public BrokerMapParser(final InputStream config) {
        final Map<String, Object> yaml = new Yaml().load(config);
        final List<BrokerMapping> brokers = ((List<Object>) yaml.get("proxies")).stream()
                .map(it -> (Map<String, Object>) it)
                .map(BrokerMapParser::toBrokerMapping)
                .collect(Collectors.toList());
        brokerMap = new BrokerMap(brokers);
    }

    @SuppressWarnings("unchecked")
    private static BrokerMapping toBrokerMapping(final Map<String, Object> o) {
        return new BrokerMapping(
                toEndpoint((Map<String, Object>) o.get("broker")),
                toEndpoint((Map<String, Object>) o.get("proxy"))
        );
    }

    private static BrokerMapping.Endpoint toEndpoint(final Map<String, Object> endpoint) {
        return new BrokerMapping.Endpoint((String) endpoint.get("hostname"), (int) endpoint.get("port"));
    }

    public BrokerMap getBrokerMap() {
        return brokerMap;
    }
}
