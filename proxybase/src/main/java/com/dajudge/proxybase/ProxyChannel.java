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
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.util.UUID;

import static com.dajudge.proxybase.LogHelper.withChannelId;
import static com.dajudge.proxybase.ProxySslHandlerFactory.createSslHandler;

public class ProxyChannel {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannel.class);
    private boolean initialized = false;
    private final Endpoint endpoint;
    private final UpstreamConfig sslConfig;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final DownstreamChannelFactory downstreamSinkFactory;
    private final CertificateAuthority certificateAuthority;
    private final FilterPairFactory<ByteBuf> filterPairFactory;
    private Channel channel;

    ProxyChannel(
            final Endpoint endpoint,
            final UpstreamConfig sslConfig,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final DownstreamChannelFactory downstreamSinkFactory,
            final CertificateAuthority certificateAuthority,
            final FilterPairFactory<ByteBuf> filterPairFactory
    ) {
        this.endpoint = endpoint;
        this.sslConfig = sslConfig;
        this.bossGroup = bossGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.downstreamSinkFactory = downstreamSinkFactory;
        this.certificateAuthority = certificateAuthority;
        this.filterPairFactory = filterPairFactory;
    }

    public void start() {
        if (initialized) {
            return;
        }
        initialized = true;
        LOG.info("Starting proxy channel {}", endpoint);
        try {
            channel = new ServerBootstrap()
                    .group(bossGroup, upstreamWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(createProxyInitializer(sslConfig))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(endpoint.getPort())
                    .sync()
                    .channel();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ChannelInitializer<SocketChannel> createProxyInitializer(
            final UpstreamConfig upstreamConfig
    ) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel ch) {
                final String channelId = UUID.randomUUID().toString();
                withChannelId(channelId, () -> {
                    final ChannelPipeline pipeline = ch.pipeline();
                    LOG.debug("Incoming connection on {} from {}", ch.localAddress(), ch.remoteAddress());
                    pipeline.addLast("ssl", createSslHandler(upstreamConfig));
                    pipeline.addLast(createDownstreamHandler(channelId, new SocketChannelSink(ch)));
                });
            }
        };
    }

    private ForwardingInboundHandler createDownstreamHandler(
            final String channelId,
            final Sink<ByteBuf> upstreamSink
    ) {
        return new ForwardingInboundHandler(channelId, certSupplier -> {
            try {
                return downstreamSinkFactory.create(
                        channelId,
                        upstreamSink,
                        filterPairFactory,
                        getClientKeystore(certSupplier)
                );
            } catch (final RuntimeException e) {
                LOG.error("Failed to create downstream channel", e);
                throw e;
            }
        });
    }

    private KeyStoreWrapper getClientKeystore(final UpstreamCertificateSupplier certSupplier) {
        try {
            return certificateAuthority.createClientCertificate(certSupplier);
        } catch (final SSLPeerUnverifiedException e) {
            throw new RuntimeException("Client did not provide valid certificate", e);
        }
    }

    public ChannelFuture close() {
        return channel.close();
    }

    public int getPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    public String getHost() {
        return endpoint.getHost();
    }

    private static class SocketChannelSink implements Sink<ByteBuf> {
        private final SocketChannel ch;

        public SocketChannelSink(final SocketChannel ch) {
            this.ch = ch;
        }

        @Override
        public ChannelFuture close() {
            LOG.trace("Closing upstream channel.");
            return ch.close().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    LOG.trace("Upstream channel closed.");
                } else {
                    LOG.warn("Cloud not close upstream channel.", future.cause());
                }
            });
        }

        @Override
        public void accept(final ByteBuf buffer) {
            ch.writeAndFlush(buffer.copy()).addListener((ChannelFutureListener) future -> {
                buffer.release();
                if (!future.isSuccess()) {
                    LOG.error("Failed to send {} bytes upstream.", buffer.readableBytes(), future.cause());
                } else {
                    LOG.trace("Sent {} bytes upstream.", buffer.readableBytes());
                }
            });
        }
    }
}
