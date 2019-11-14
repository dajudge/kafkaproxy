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