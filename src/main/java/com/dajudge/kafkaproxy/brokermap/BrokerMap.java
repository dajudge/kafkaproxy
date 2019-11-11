package com.dajudge.kafkaproxy.brokermap;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class BrokerMap {
    private final Map<String, BrokerMapping> byBrokerEndpoint;
    private final Map<String, BrokerMapping> byName;

    public BrokerMap(final Map<String, BrokerMapping> mappings) {
        this.byBrokerEndpoint = mappings;
        byName = mappings.values().stream().collect(toMap(
                BrokerMapping::getName,
                it -> it
        ));
    }

    public BrokerMapping getMappingByBrokerEndpoint(final String host, final int port) {
        return byBrokerEndpoint.get(host + ":" + port);
    }

    public BrokerMapping getMappingByBrokerName(final String name) {
        return byName.get(name);
    }

    @Override
    public String toString() {
        return "BrokerMap{" +
                "mappings=" + byBrokerEndpoint +
                '}';
    }
}
