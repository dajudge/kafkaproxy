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
import com.dajudge.kafkaproxy.config.KafkaBrokerConfigSource.KafkaBrokerConfig;
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
import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import com.dajudge.proxybase.RelayingChannelInboundHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dajudge.kafkaproxy.ca.CertificateAuthorityFactory.createCertificateAuthority;
import static com.dajudge.proxybase.DownstreamSslHandlerFactory.createDownstreamSslHandler;
import static com.dajudge.proxybase.UpstreamSslHandlerFactory.createUpstreamSslHandler;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedMap;

public class KafkaProxyApplication extends ProxyApplication {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProxyApplication.class);

    public KafkaProxyApplication(final ApplicationConfig appConfig) {
        super(createProxyRuntime(appConfig));
        LOG.trace("Kafkaproxy init complete");
    }

    private static Consumer<ProxyChannelFactory> createProxyRuntime(final ApplicationConfig appConfig) {
        final BrokerConfig brokerConfig = appConfig.get(BrokerConfig.class);
        final BrokerMapper brokerMapper = new BrokerMapper(brokerConfig);
        final UpstreamSslConfig upstreamSslConfig = appConfig.get(UpstreamSslConfig.class);
        final DownstreamSslConfig downstreamSslConfig = appConfig.get(KafkaBrokerConfig.class)
                .getDownstreamConfig();
        final CertificateAuthority ca = createCertificateAuthority(appConfig);
        return channelFactory -> {
            final Map<Endpoint, BrokerMapping> activeProxies = synchronizedMap(new HashMap<>());
            final Function<Endpoint, BrokerMapping> brokerResolver = new Function<Endpoint, BrokerMapping>() {
                @Override
                public BrokerMapping apply(final Endpoint brokerEndpoint) {
                    return activeProxies.computeIfAbsent(brokerEndpoint, createProxy(
                            channelFactory,
                            brokerMapper,
                            this,
                            upstreamSslConfig,
                            downstreamSslConfig,
                            ca
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
            final UpstreamSslConfig upstreamSslConfig,
            final DownstreamSslConfig downstreamSslConfig,
            final CertificateAuthority ca
    ) {
        return brokerEndpoint -> {
            final BrokerMapping mapping = brokerMapper.getBrokerMapping(brokerEndpoint);
            final Endpoint proxyEndpoint = mapping.getProxy();
            LOG.info("Initializing proxy {} for broker {}", proxyEndpoint, brokerEndpoint);
            final ProxyChannelInitializer initializer = (upstreamChannel, downstreamChannel) -> {
                configureUpstream(upstreamSslConfig, upstreamChannel, downstreamChannel);
                final KeyStoreWrapper clientKeyStore = getClientKeystore(ca, () -> clientCertFrom(upstreamChannel));
                configureDownstream(
                        brokerEndpoint,
                        downstreamSslConfig,
                        upstreamChannel,
                        downstreamChannel,
                        brokerResolver,
                        clientKeyStore
                );
            };
            channelFactory.createProxyChannel(
                    new Endpoint(BIND_ADDRESS),
                    brokerEndpoint,
                    initializer
            );
            return mapping;
        };
    }

    private static void configureUpstream(
            final UpstreamSslConfig sslConfig,
            final Channel upstreamChannel,
            final Channel downstreamChannel
    ) {
        upstreamChannel.pipeline().addLast(new DecodingKafkaMessageInboundHandler());
        upstreamChannel.pipeline().addLast(new EncodingKafkaMessageOutboundHandler());
        upstreamChannel.pipeline().addLast(new RelayingChannelInboundHandler("downstream", downstreamChannel));
        upstreamChannel.pipeline().addAfter(
                LOGGING_CONTEXT_HANDLER,
                "SSL",
                createUpstreamSslHandler(sslConfig)
        );
    }

    private static void configureDownstream(
            final Endpoint downstream,
            final DownstreamSslConfig sslConfig,
            final Channel upstreamChannel,
            final Channel downstreamChannel,
            final Function<Endpoint, BrokerMapping> brokerResolver,
            final KeyStoreWrapper clientKeyStore
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
        downstreamChannel.pipeline().addAfter(
                LOGGING_CONTEXT_HANDLER,
                "SSL",
                createDownstreamSslHandler(sslConfig, downstream, clientKeyStore)
        );
    }

    private static KeyStoreWrapper getClientKeystore(
            final CertificateAuthority certificateAuthority,
            final UpstreamCertificateSupplier certSupplier
    ) {
        try {
            return certificateAuthority.createClientCertificate(certSupplier);
        } catch (final SSLPeerUnverifiedException e) {
            throw new RuntimeException("Client did not provide valid certificate", e);
        }
    }

    private static X509Certificate clientCertFrom(final Channel channel) throws SSLPeerUnverifiedException {
        final ChannelHandler sslHandler = channel.pipeline().get("SSL");
        if (sslHandler instanceof SslHandler) {
            final SSLSession session = ((SslHandler) sslHandler).engine().getSession();
            final Certificate[] clientCerts = session.getPeerCertificates();
            return (X509Certificate) clientCerts[0];
        } else {
            throw new SSLPeerUnverifiedException("Upstream SSL not enabled");
        }
    }

    public static KafkaProxyApplication create(final Environment environment) {
        return new KafkaProxyApplication(new ApplicationConfig(environment));
    }
}
