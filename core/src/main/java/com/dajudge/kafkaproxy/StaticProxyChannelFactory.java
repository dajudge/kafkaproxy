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

import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.broker.BrokerConfig;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticProxyChannelFactory implements ProxyChannelManager.ProxyChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(StaticProxyChannelFactory.class);
    private final ApplicationConfig appConfig;
    private final NioEventLoopGroup downstreamWorkerGroup;
    private final NioEventLoopGroup serverWorkerGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;

    public StaticProxyChannelFactory(
            final ApplicationConfig appConfig,
            final NioEventLoopGroup downstreamWorkerGroup,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup
    ) {
        this.appConfig = appConfig;
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.serverWorkerGroup = serverWorkerGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
    }

    @Override
    public ProxyChannel create(final ProxyChannelManager manager, final String brokerHostname, final int brokerPort) {
        final BrokerMapping brokerToProxy = appConfig.get(BrokerConfig.class).getBrokerMap()
                .getByBrokerEndpoint(brokerHostname, brokerPort);
        if (brokerToProxy == null) {
            final String broker = brokerHostname + ":" + brokerPort;
            throw new IllegalArgumentException("No proxy configuration provided for " + broker);
        }
        final ForwardChannelFactory forwardChannelFactory = new DownstreamChannelFactory(
                manager,
                brokerToProxy.getBroker().getHost(),
                brokerToProxy.getBroker().getPort(),
                appConfig,
                downstreamWorkerGroup
        );
        final ProxyChannel proxyChannel = new ProxyChannel(
                brokerToProxy.getProxy().getHost(),
                brokerToProxy.getProxy().getPort(),
                appConfig,
                serverWorkerGroup,
                upstreamWorkerGroup,
                forwardChannelFactory
        );
        LOG.info(
                "Proxying {}:{} as {}:{}",
                brokerToProxy.getBroker().getHost(),
                brokerToProxy.getBroker().getPort(),
                brokerToProxy.getProxy().getHost(),
                brokerToProxy.getProxy().getPort()
        );
        return proxyChannel;
    }

    @Override
    public void bootstrap(final ProxyChannelManager manager) {
        appConfig.get(BrokerConfig.class).getBrokerMap().getAll().forEach(broker ->
                manager.getByBrokerEndpoint(broker.getBroker().getHost(), broker.getBroker().getPort())
        );
    }
}
