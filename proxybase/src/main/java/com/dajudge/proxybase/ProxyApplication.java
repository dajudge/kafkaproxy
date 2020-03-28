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

import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.config.DownstreamConfig;
import com.dajudge.proxybase.config.UpstreamConfig;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public abstract class ProxyApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApplication.class);
    private final UpstreamConfig upstreamConfig;
    private final DownstreamConfig downstreamConfig;
    private final CertificateAuthority certificateAuthority;
    private Runnable shutdownRunnable;

    protected ProxyApplication(
            final UpstreamConfig upstreamConfig,
            final DownstreamConfig downstreamConfig,
            final CertificateAuthority certificateAuthority
    ) {
        this.upstreamConfig = upstreamConfig;
        this.downstreamConfig = downstreamConfig;
        this.certificateAuthority = certificateAuthority;
    }

    public void shutdown() {
        if (shutdownRunnable == null) {
            throw new IllegalStateException("must invoke start() first");
        }
        shutdownRunnable.run();
    }

    public ProxyApplication start() {
        final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final ProxyChannelFactory proxyChannelFactory = new ProxyChannelFactory(
                downstreamWorkerGroup,
                serverWorkerGroup,
                upstreamWorkerGroup,
                upstreamConfig,
                downstreamConfig,
                certificateAuthority
        );
        final Collection<ProxyChannel> proxyChannels = initializeProxyChannels(proxyChannelFactory);
        shutdownRunnable = () -> {
            proxyChannels.stream()
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
        return this;
    }

    protected abstract Collection<ProxyChannel> initializeProxyChannels(
            final ProxyChannelFactory proxyChannelFactory
    );
}
