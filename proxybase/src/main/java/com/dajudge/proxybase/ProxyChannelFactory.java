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

package com.dajudge.proxybase;

import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.config.DownstreamConfig;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannelFactory.class);
    private final NioEventLoopGroup downstreamWorkerGroup;
    private final NioEventLoopGroup serverWorkerGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final UpstreamConfig upstreamConfig;
    private final DownstreamConfig downstreamConfig;
    private final CertificateAuthority certificateAuthority;

    ProxyChannelFactory(
            final NioEventLoopGroup downstreamWorkerGroup,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final UpstreamConfig upstreamConfig,
            final DownstreamConfig downstreamConfig,
            final CertificateAuthority certificateAuthority
    ) {
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.serverWorkerGroup = serverWorkerGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.upstreamConfig = upstreamConfig;
        this.downstreamConfig = downstreamConfig;
        this.certificateAuthority = certificateAuthority;
    }

    public ProxyChannel createProxyChannel(
            final Endpoint upstreamEndpoint,
            final Endpoint downstreamEndpoint,
            final FilterPairFactory<ByteBuf> filterPairFactory
    ) {
        final DownstreamChannelFactory downstreamSinkFactory = new DownstreamChannelFactory(
                downstreamEndpoint,
                downstreamConfig,
                downstreamWorkerGroup
        );
        final ProxyChannel proxyChannel = new ProxyChannel(
                upstreamEndpoint,
                upstreamConfig,
                serverWorkerGroup,
                upstreamWorkerGroup,
                downstreamSinkFactory,
                certificateAuthority,
                filterPairFactory
        );
        LOG.info("Proxying {} as {}", downstreamEndpoint, upstreamEndpoint);
        return proxyChannel;
    }

}
