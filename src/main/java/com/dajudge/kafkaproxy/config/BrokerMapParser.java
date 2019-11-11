package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrokerMapParser {
    private final BrokerMap brokerMap;

    @SuppressWarnings("unchecked")
    public BrokerMapParser(final InputStream config) {
        final Map<String, Object> yaml = new Yaml().load(config);
        brokerMap = new BrokerMap(new HashMap<String, BrokerMapping>() {{
            ((List<Object>) yaml.get("proxies")).stream()
                    .map(it -> (Map<String, Object>) it)
                    .map(BrokerMapParser::toBrokerMapping)
                    .forEach(m -> put(m.getProxy().getHost() + ":" + m.getProxy().getPort(), m));
        }});
    }

    @SuppressWarnings("unchecked")
    private static BrokerMapping toBrokerMapping(final Map<String, Object> o) {
        return new BrokerMapping(
                (String)o.get("name"),
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
