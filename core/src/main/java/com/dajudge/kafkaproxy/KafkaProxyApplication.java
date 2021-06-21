/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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
import com.dajudge.kafkaproxy.config.BrokerConfigSource.BrokerConfig;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.protocol.DecodingKafkaMessageInboundHandler;
import com.dajudge.kafkaproxy.protocol.EncodingKafkaMessageOutboundHandler;
import com.dajudge.kafkaproxy.protocol.KafkaRequestStore;
import com.dajudge.kafkaproxy.protocol.RewritingKafkaMessageDuplexHandler;
import com.dajudge.kafkaproxy.protocol.rewrite.CompositeRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.FindCoordinatorRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.MetadataRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.ResponseRewriter;
import com.dajudge.proxybase.ProxyApplication;
import com.dajudge.proxybase.ProxyChannelFactory;
import com.dajudge.proxybase.ProxyChannelFactory.ProxyChannelInitializer;
import com.dajudge.proxybase.RelayingChannelInboundHandler;
import com.dajudge.proxybase.certs.Filesystem;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.dajudge.proxybase.DownstreamSslHandlerFactory.createDownstreamSslHandler;
import static com.dajudge.proxybase.UpstreamSslHandlerFactory.createUpstreamSslHandler;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedMap;

public class KafkaProxyApplication extends ProxyApplication {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProxyApplication.class);

    public KafkaProxyApplication(
            final ApplicationConfig appConfig,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        super(createProxyRuntime(appConfig, clock, filesystem));
        LOG.trace("Kafkaproxy init complete");
    }

    private static Consumer<ProxyChannelFactory> createProxyRuntime(
            final ApplicationConfig appConfig,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        final BrokerConfig brokerConfig = appConfig.require(BrokerConfig.class);
        final BrokerMapper brokerMapper = new BrokerMapper(brokerConfig);
        final Optional<UpstreamSslConfig> upstreamSslConfig = appConfig.optional(UpstreamSslConfig.class);
        final Optional<DownstreamSslConfig> downstreamSslConfig = appConfig.optional(DownstreamSslConfig.class);
        return channelFactory -> {
            final Map<Endpoint, BrokerMapping> activeProxies = synchronizedMap(new HashMap<>());
            final Function<Endpoint, BrokerMapping> brokerResolver = new Function<Endpoint, BrokerMapping>() {
                @Override
                public BrokerMapping apply(final Endpoint brokerEndpoint) {
                    return activeProxies.computeIfAbsent(brokerEndpoint, createProxy(
                            channelFactory,
                            brokerMapper,
                            this,
                            brokerConfig.getBindAddress(),
                            upstreamSslConfig,
                            downstreamSslConfig,
                            clock,
                            filesystem
                    ));
                }
            };
            brokerMapper.getBootstrapBrokers().forEach(brokerResolver::apply);
        };
    }

    private static Function<Endpoint, BrokerMapping> createProxy(
            final ProxyChannelFactory channelFactory,
            final BrokerMapper brokerMapper,
            final Function<Endpoint, BrokerMapping> brokerResolver,
            final String bindAddress,
            final Optional<UpstreamSslConfig> upstreamSslConfig,
            final Optional<DownstreamSslConfig> downstreamSslConfig,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        return brokerEndpoint -> {
            final BrokerMapping mapping = brokerMapper.getBrokerMapping(brokerEndpoint);
            final Endpoint proxyEndpoint = mapping.getProxy();
            LOG.info("Initializing proxy {} for broker {}", proxyEndpoint, brokerEndpoint);
            final ProxyChannelInitializer initializer = (upstreamChannel, downstreamChannel) -> {
                configureUpstream(
                        upstreamSslConfig,
                        upstreamChannel,
                        downstreamChannel,
                        clock,
                        filesystem
                );
                configureDownstream(
                        brokerEndpoint,
                        downstreamSslConfig,
                        upstreamChannel,
                        downstreamChannel,
                        brokerResolver,
                        clock, filesystem
                );
            };
            channelFactory.createProxyChannel(
                    new Endpoint(bindAddress, proxyEndpoint.getPort()),
                    brokerEndpoint,
                    initializer
            );
            return mapping;
        };
    }

    private static void configureUpstream(
            final Optional<UpstreamSslConfig> sslConfig,
            final Channel upstreamChannel,
            final Channel downstreamChannel,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        upstreamChannel.pipeline().addLast(new DecodingKafkaMessageInboundHandler());
        upstreamChannel.pipeline().addLast(new EncodingKafkaMessageOutboundHandler());
        upstreamChannel.pipeline().addLast(new RelayingChannelInboundHandler("downstream", downstreamChannel));
        sslConfig.ifPresent(it -> upstreamChannel.pipeline().addAfter(
                LOGGING_CONTEXT_HANDLER,
                "SSL",
                createUpstreamSslHandler(it, clock, filesystem)
        ));
    }

    private static void configureDownstream(
            final Endpoint downstream,
            final Optional<DownstreamSslConfig> sslConfig,
            final Channel upstreamChannel,
            final Channel downstreamChannel,
            final Function<Endpoint, BrokerMapping> brokerResolver,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        final ResponseRewriter rewriter = new CompositeRewriter(asList(
                new MetadataRewriter(brokerResolver),
                new FindCoordinatorRewriter(brokerResolver)
        ));
        final KafkaRequestStore kafkaRequestStore = new KafkaRequestStore(rewriter);
        downstreamChannel.pipeline().addLast(new EncodingKafkaMessageOutboundHandler());
        downstreamChannel.pipeline().addLast(new DecodingKafkaMessageInboundHandler());
        downstreamChannel.pipeline().addLast(new RewritingKafkaMessageDuplexHandler(kafkaRequestStore));
        downstreamChannel.pipeline().addLast(new RelayingChannelInboundHandler("upstream", upstreamChannel));
        sslConfig.ifPresent(it -> {
            final ChannelHandler sslHandler = createDownstreamSslHandler(it, downstream, clock, filesystem)
                    .apply(downstreamChannel.pipeline().channel());
            downstreamChannel.pipeline().addAfter(
                    LOGGING_CONTEXT_HANDLER,
                    "SSL",
                    sslHandler
            );
        });
    }

    public static KafkaProxyApplication create(
            final Environment environment,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        return new KafkaProxyApplication(new ApplicationConfig(environment), clock, filesystem);
    }
}
