package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.BrokerMapParser;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;
import java.io.FileInputStream;

public class Startup {
    private static final String PREFIX = "KAFKAPROXY_";
    private static final String PROP_BROKERMAP_LOCATION = PREFIX + "BROKERMAP_LOCATION";
    private static final String PROP_BROKER_TO_PROXY = PREFIX + "_BROKER_TO_PROXY";
    private static final String DEFAULT_BROKERMAP_LOCATION = "/etc/kafkaproxy/mappings.yml";

    public static void main(final String argv[]) {
        final File brokerMapFile = new File(System.getProperty(PROP_BROKERMAP_LOCATION, DEFAULT_BROKERMAP_LOCATION));
        final BrokerMap brokerMap = readBrokerMap(brokerMapFile);
        final BrokerMapping brokerToProxy = brokerMap.getMappingByBrokerName(System.getProperty(PROP_BROKER_TO_PROXY));
        final KafkaSslConfig kafkaSslConfig = new KafkaSslConfig(false, null, null, false);
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final ProxySslConfig proxySslConfig = new ProxySslConfig(false, null, null, null, null, null);
        final ProxyChannel channel = new ProxyChannel(
                brokerToProxy.getProxy().getPort(),
                brokerToProxy.getBroker().getHost(),
                brokerToProxy.getBroker().getPort(),
                proxySslConfig,
                kafkaSslConfig,
                brokerMap,
                bossGroup,
                upstreamWorkerGroup,
                downstreamWorkerGroup
        );
        Runtime.getRuntime().addShutdownHook(new Thread(channel::close));
    }

    private static BrokerMap readBrokerMap(final File brokerMapFile) {
        try (final FileInputStream inputStream = new FileInputStream(brokerMapFile)) {
            return new BrokerMapParser(inputStream).getBrokerMap();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to read broker map", e);
        }
    }
}
