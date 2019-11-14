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
                (String) o.get("name"),
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
