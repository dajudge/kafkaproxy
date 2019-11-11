package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ConfigParserTest {
    @Test
    public void parses_brokers() throws IOException {
        try (final InputStream config = config("config1.yml")) {
            final BrokerMap brokerMap = new BrokerMapParser(config).getBrokerMap();
            assertEquals(9092, brokerMap.getMappingByBrokerEndpoint("kafka.example.com", 39092).getBroker().getPort());
            assertEquals("broker1.kafka.local", brokerMap.getMappingByBrokerEndpoint("kafka.example.com", 39092).getBroker().getHost());
            assertEquals(9092, brokerMap.getMappingByBrokerEndpoint("kafka.example.com", 39093).getBroker().getPort());
            assertEquals("broker2.kafka.local", brokerMap.getMappingByBrokerEndpoint("kafka.example.com", 39093).getBroker().getHost());
        }
    }

    private static final InputStream config(final String configName) {
        return ConfigParserTest.class.getClassLoader().getResourceAsStream("configs/" + configName);
    }
}
