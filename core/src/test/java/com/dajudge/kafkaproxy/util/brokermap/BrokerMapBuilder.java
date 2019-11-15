package com.dajudge.kafkaproxy.util.brokermap;

import com.dajudge.kafkaproxy.util.PortFinder;
import com.dajudge.kafkaproxy.util.kafka.KafkaCluster;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public final class BrokerMapBuilder {
    private BrokerMapBuilder() {
    }

    public static byte[] brokerMapFile(final KafkaCluster kafkaCluster) {
        try (final PortFinder portFinder = new PortFinder()) {
            final List<Object> proxies = kafkaCluster.getBrokers().entrySet().stream()
                    .map(e -> entryFor(e.getKey(), e.getValue(), "localhost", portFinder.nextPort()))
                    .collect(toList());
            final HashMap<String, Object> root = new HashMap<String, Object>() {{
                put("proxies", proxies);
            }};
            return new Yaml().dump(root).getBytes(UTF_8);
        }
    }

    private static HashMap<String, Object> entryFor(
            final String brokerHostname,
            final int brokerPort,
            final String proxyHostname,
            final int proxyPort
    ) {
        return new HashMap<String, Object>() {{
            put("name", brokerHostname);
            put("proxy", endpointFor(proxyHostname, proxyPort));
            put("broker", endpointFor(brokerHostname, brokerPort));
        }};
    }

    private static HashMap<String, Object> endpointFor(final String hostname, final int port) {
        return new HashMap<String, Object>() {{
            put("hostname", hostname);
            put("port", port);
        }};
    }
}
