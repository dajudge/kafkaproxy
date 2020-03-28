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

package com.dajudge.proxybase;

import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
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
    private final ClientCertificateAuthority clientCertificateAuthority;

    public ProxyChannelFactory(
            final NioEventLoopGroup downstreamWorkerGroup,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final UpstreamSslConfig upstreamSslConfig,
            final DownstreamSslConfig downstreamSslConfig,
            final ClientCertificateAuthority clientCertificateAuthority
    ) {
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.serverWorkerGroup = serverWorkerGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.upstreamSslConfig = upstreamSslConfig;
        this.downstreamSslConfig = downstreamSslConfig;
        this.clientCertificateAuthority = clientCertificateAuthority;
    }

    public ProxyChannel createProxyChannel(
            final Endpoint upstreamEndpoint,
            final Endpoint downstreamEndpoint,
            final FilterFactory<ByteBuf> upstreamFilterFactory,
            final FilterFactory<ByteBuf> downstreamFilterFactory
    ) {
        final DownstreamChannelFactory downstreamSinkFactory = new DownstreamChannelFactory(
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
