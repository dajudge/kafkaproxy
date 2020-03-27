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

package com.dajudge.kafkaproxy.networking;

import com.dajudge.kafkaproxy.ClientCertificateAuthorityImpl;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.DownstreamSinkFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.UpstreamSslConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannelFactory.class);
    private final NioEventLoopGroup downstreamWorkerGroup;
    private final NioEventLoopGroup serverWorkerGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final UpstreamSslConfig upstreamSslConfig;
    private final DownstreamSslConfig downstreamSslConfig;
    private final ClientCertificateAuthorityImpl clientCertificateAuthority;

    public ProxyChannelFactory(
            final NioEventLoopGroup downstreamWorkerGroup,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final UpstreamSslConfig upstreamSslConfig,
            final DownstreamSslConfig downstreamSslConfig,
            final ClientCertificateAuthorityImpl clientCertificateAuthority
    ) {
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.serverWorkerGroup = serverWorkerGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.upstreamSslConfig = upstreamSslConfig;
        this.downstreamSslConfig = downstreamSslConfig;
        this.clientCertificateAuthority = clientCertificateAuthority;
    }

    public ProxyChannel createProxyChannel(final FilterFactory<ByteBuf> upstreamFilterFactory, final FilterFactory<ByteBuf> downstreamFilterFactory, final Endpoint downstreamEndpoint, final Endpoint upstreamEndpoint) {
        final DownstreamSinkFactory downstreamSinkFactory = new DownstreamChannelFactory(
                downstreamEndpoint,
                downstreamSslConfig,
                downstreamWorkerGroup,
                clientCertificateAuthority
        );
        final ProxyChannel proxyChannel = new ProxyChannel(
                upstreamEndpoint,
                upstreamSslConfig,
                serverWorkerGroup,
                upstreamWorkerGroup,
                downstreamSinkFactory,
                upstreamFilterFactory,
                downstreamFilterFactory
        );
        LOG.info("Proxying {} as {}", downstreamEndpoint, upstreamEndpoint);
        return proxyChannel;
    }

}
