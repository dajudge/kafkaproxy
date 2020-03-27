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

package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.ca.UpstreamCertificateSupplier;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProxyChannel {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannel.class);
    private final String hostname;
    private boolean initialized = false;
    private final int port;
    private final ApplicationConfig appConfig;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final ForwardChannelFactory forwardChannelFactory;

    private Channel channel;

    public ProxyChannel(
            final String hostname,
            final int port,
            final ApplicationConfig appConfig,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final ForwardChannelFactory forwardChannelFactory
    ) {
        this.hostname = hostname;
        this.port = port;
        this.appConfig = appConfig;
        this.bossGroup = bossGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.forwardChannelFactory = forwardChannelFactory;
    }

    public void start() {
        if (initialized) {
            return;
        }
        initialized = true;
        LOG.info("Starting proxy channel {}:{}", hostname, port);
        try {
            final ChannelInitializer<SocketChannel> childHandler = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) {
                    final ChannelPipeline pipeline = ch.pipeline();
                    LOG.trace("Incoming connection: {}", ch.remoteAddress());
                    final ForwardChannel<ByteBuf> upstreamSink = new SocketChannelSink(ch);
                    final ProxySslConfig proxySslConfig = appConfig.get(ProxySslConfig.class);
                    pipeline.addLast("ssl", ProxySslHandlerFactory.createHandler(proxySslConfig));
                    final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> downstreamFactory = certSupplier -> {
                        try {
                            final ForwardChannel<ByteBuf> forwardChannel = forwardChannelFactory.create(
                                    certSupplier,
                                    upstreamSink
                            );
                            ch.closeFuture().addListener((ChannelFutureListener) future -> forwardChannel.close());
                            return forwardChannel::accept;
                        } catch (final RuntimeException e) {
                            LOG.error("Failed to create downstream channel", e);
                            throw e;
                        }
                    };
                    pipeline.addLast(new ProxyServerHandler(downstreamFactory));
                }
            };
            channel = new ServerBootstrap()
                    .group(bossGroup, upstreamWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(childHandler)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(port).sync().channel();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public ChannelFuture close() {
        return channel.close();
    }

    public int getPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    public String getHost() {
        return hostname;
    }

    private static class SocketChannelSink implements ForwardChannel<ByteBuf> {
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
