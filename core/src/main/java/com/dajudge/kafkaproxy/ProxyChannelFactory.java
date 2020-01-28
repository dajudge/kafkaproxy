/*
 * Copyright 2019-2020 Alex Stockinger
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
import com.dajudge.kafkaproxy.brokermap.BrokerMapping.Endpoint;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class ProxyChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannelFactory.class);
    private final ApplicationConfig appConfig;
    private final BrokerMappingStrategy brokerMappingStrategy;
    private final NioEventLoopGroup downstreamWorkerGroup;
    private final NioEventLoopGroup serverWorkerGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;

    public ProxyChannelFactory(
            final ApplicationConfig appConfig,
            final BrokerMappingStrategy brokerMappingStrategy,
            final NioEventLoopGroup downstreamWorkerGroup,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup
    ) {
        this.appConfig = appConfig;
        this.brokerMappingStrategy = brokerMappingStrategy;
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.serverWorkerGroup = serverWorkerGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
    }

    public ProxyChannel create(final ProxyChannelManager manager, final Endpoint endpoint) {
        final BrokerMapping brokerToProxy = brokerMappingStrategy.getBrokerMapping(endpoint);
        if (brokerToProxy == null) {
            throw new IllegalArgumentException("No proxy configuration provided for " + endpoint);
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

    public Set<BrokerMapping> bootstrap(final ProxyChannelManager manager) {
        return brokerMappingStrategy.getBootstrapBrokers().stream()
                .map(BrokerMapping::getBroker)
                .map(manager::getByBrokerEndpoint)
                .collect(toSet());
    }

    interface BrokerMappingStrategy {
        BrokerMapping getBrokerMapping(final Endpoint endpoint);

        Collection<BrokerMapping> getBootstrapBrokers();
    }
}
