package com.dajudge.kafkaproxy.brokermap;

import java.util.Map;

public class BrokerMapper {
    private final Map<String, BrokerMapping> mappings;

    public BrokerMapper(final Map<String, BrokerMapping> mappings) {
        this.mappings = mappings;
    }

    public BrokerMapping map(final String host, final int port) {
        return mappings.get(host + ":" + port);
    }
}
