package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.BrokerMapParser;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.util.stream.Collectors.toList;

public class Startup {
    private static final Logger LOG = LoggerFactory.getLogger(Startup.class);
    private static final String PREFIX = "KAFKAPROXY_";
    private static final String PROP_BROKERMAP_LOCATION = PREFIX + "BROKERMAP_LOCATION";
    private static final String PROP_PROXIED_BROKERS = PREFIX + "PROXIED_BROKERS";
    private static final String PROP_KAFKA_SSL_ENABLED = PREFIX + "KAFKA_SSL_ENABLED";
    private static final String PROP_CLIENT_SSL_ENABLED = PREFIX + "CLIENT_SSL_ENABLED";
    private static final String DEFAULT_BROKERMAP_LOCATION = "/etc/kafkaproxy/brokermap.yml";
    private static final String DEFAULT_PROXIED_BROKERS = "*";
    private static final String DEFAULT_KAFKA_SSL_ENABLED = "false";
    private static final String DEFAULT_CLIENT_SSL_ENABLED = "false";

    public static void main(final String argv[]) {
        final File brokerMapFile = getBrokerMapFile();
        final BrokerMap brokerMap = readBrokerMap(brokerMapFile);
        final Collection<BrokerMapping> brokersToProxy = getBrokersToProxy(brokerMap);
        final KafkaSslConfig kafkaSslConfig = getKafkaSslConfig();
        final ProxySslConfig proxySslConfig = getProxySslConfig();
        final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final Collection<ProxyChannel> proxies = brokersToProxy.stream()
                .map(brokerToProxy -> {
                    final ForwardChannelFactory forwardChannelFactory = new DownstreamChannelFactory(
                            brokerMap,
                            brokerToProxy.getBroker().getHost(),
                            brokerToProxy.getBroker().getPort(),
                            kafkaSslConfig,
                            downstreamWorkerGroup
                    );
                    return new ProxyChannel(
                            brokerToProxy.getProxy().getPort(),
                            proxySslConfig,
                            serverWorkerGroup,
                            upstreamWorkerGroup,
                            forwardChannelFactory
                    );
                }).collect(toList());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            proxies.stream()
                    .map(ProxyChannel::close)
                    .collect(toList())
                    .forEach(future -> {
                        try {
                            future.sync();
                        } catch (final Exception e) {
                            LOG.error("Failed to sync with proxy channel", e);
                        }
                    });
            serverWorkerGroup.shutdownGracefully();
            upstreamWorkerGroup.shutdownGracefully();
            downstreamWorkerGroup.shutdownGracefully();
        }));
    }

    private static ProxySslConfig getProxySslConfig() {
        final boolean enabled = parseBoolean(System.getProperty(PROP_CLIENT_SSL_ENABLED, DEFAULT_CLIENT_SSL_ENABLED));
        if (!enabled) {
            return ProxySslConfig.DISABLED;
        }
        return ProxySslConfig.DISABLED;
    }

    private static KafkaSslConfig getKafkaSslConfig() {
        final boolean enabled = parseBoolean(System.getProperty(PROP_KAFKA_SSL_ENABLED, DEFAULT_KAFKA_SSL_ENABLED));
        if (!enabled) {
            return KafkaSslConfig.DISABLED;
        }
        return KafkaSslConfig.DISABLED;
    }

    private static File getBrokerMapFile() {
        return new File(System.getProperty(PROP_BROKERMAP_LOCATION, DEFAULT_BROKERMAP_LOCATION));
    }

    private static List<BrokerMapping> getBrokersToProxy(final BrokerMap brokerMap) {
        final String property = System.getProperty(PROP_PROXIED_BROKERS, DEFAULT_PROXIED_BROKERS);
        if ("*".equals(property)) {
            return brokerMap.getAll();
        }
        return Stream.of(property.split(","))
                .map(String::trim)
                .map(brokerMap::getMappingByBrokerName)
                .collect(toList());
    }

    private static BrokerMap readBrokerMap(final File brokerMapFile) {
        try (final FileInputStream inputStream = new FileInputStream(brokerMapFile)) {
            return new BrokerMapParser(inputStream).getBrokerMap();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to read broker map", e);
        }
    }
}
