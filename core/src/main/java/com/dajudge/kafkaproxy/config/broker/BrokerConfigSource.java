package com.dajudge.kafkaproxy.config.broker;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.ConfigSource;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class BrokerConfigSource implements ConfigSource<BrokerConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerConfigSource.class);
    private static final String ENV_BROKERMAP_LOCATION = PREFIX + "BROKERMAP_LOCATION";
    private static final String ENV_PROXIED_BROKERS = PREFIX + "PROXIED_BROKERS";
    private static final String DEFAULT_BROKERMAP_LOCATION = "/etc/kafkaproxy/brokermap.yml";
    private static final String DEFAULT_PROXIED_BROKERS = "*";

    @Override
    public Class<BrokerConfig> getConfigClass() {
        return BrokerConfig.class;
    }

    @Override
    public BrokerConfig parse(final Environment environment) {
        final BrokerMap brokerMap = getBrokerMap(environment);
        final Collection<BrokerMapping> brokersToProxy = getBrokersToProxy(environment, brokerMap);
        brokerMap.getAll().forEach(b -> LOG.debug("Found brokermap entry: {}", b));
        LOG.debug("Brokers to proxy: {}", brokersToProxy.stream().map(BrokerMapping::getName).collect(toList()));
        return new BrokerConfig(brokerMap, brokersToProxy);
    }

    private static List<BrokerMapping> getBrokersToProxy(final Environment environment, final BrokerMap brokerMap) {
        final String property = environment.requiredString(ENV_PROXIED_BROKERS, DEFAULT_PROXIED_BROKERS);
        if ("*".equals(property)) {
            return brokerMap.getAll();
        }
        return Stream.of(property.split(","))
                .map(String::trim)
                .map(brokerMap::getByProxyName)
                .collect(toList());
    }

    private BrokerMap getBrokerMap(final Environment environment) {
        final FileResource brokerMapFile = environment.requiredFile(ENV_BROKERMAP_LOCATION, DEFAULT_BROKERMAP_LOCATION);
        try (final InputStream inputStream = brokerMapFile.open()) {
            return new BrokerMapParser(inputStream).getBrokerMap();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to read broker map", e);
        }
    }
}
