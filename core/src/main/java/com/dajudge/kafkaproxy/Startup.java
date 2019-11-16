/*
 * Copyright 2019 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.RealEnvironment;
import com.dajudge.kafkaproxy.config.broker.BrokerConfig;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class Startup {
    private static final Logger LOG = LoggerFactory.getLogger(Startup.class);
    private final Environment environment;
    private Runnable shutdown;

    public Startup(final Environment environment) {
        this.environment = environment;
    }

    public static void main(final String argv[]) {
        final Startup startup = new Startup(new RealEnvironment());
        Runtime.getRuntime().addShutdownHook(new Thread(startup::shutdown));
        startup.start();
    }

    public void shutdown() {
        if (shutdown == null) {
            throw new IllegalStateException("must invoke start() first");
        }
        shutdown.run();
    }

    public void start() {
        final ApplicationConfig appConfig = new ApplicationConfig(environment);
        final BrokerConfig brokerConfig = appConfig.get(BrokerConfig.class);
        final KafkaSslConfig kafkaSslConfig = appConfig.get(KafkaSslConfig.class);
        final ProxySslConfig proxySslConfig = appConfig.get(ProxySslConfig.class);
        final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final Collection<ProxyChannel> proxies = brokerConfig.getBrokersToProxy().stream()
                .map(brokerToProxy -> {
                    final ForwardChannelFactory forwardChannelFactory = new DownstreamChannelFactory(
                            brokerConfig.getBrokerMap(),
                            brokerToProxy.getBroker().getHost(),
                            brokerToProxy.getBroker().getPort(),
                            kafkaSslConfig,
                            downstreamWorkerGroup
                    );
                    final ProxyChannel proxyChannel = new ProxyChannel(
                            brokerToProxy.getProxy().getPort(),
                            proxySslConfig,
                            serverWorkerGroup,
                            upstreamWorkerGroup,
                            forwardChannelFactory
                    );
                    LOG.info(
                            "Started proxy listener for {}:{} on port {}",
                            brokerToProxy.getBroker().getHost(),
                            brokerToProxy.getBroker().getPort(),
                            brokerToProxy.getProxy().getPort()
                    );
                    return proxyChannel;
                }).collect(toList());
        shutdown = () -> {
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
        };
    }
}
