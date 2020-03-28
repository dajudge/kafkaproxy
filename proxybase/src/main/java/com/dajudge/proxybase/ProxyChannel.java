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

import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static com.dajudge.proxybase.ProxySslHandlerFactory.createSslHandler;

public class ProxyChannel {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannel.class);
    private boolean initialized = false;
    private final Endpoint endpoint;
    private final UpstreamSslConfig sslConfig;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final DownstreamChannelFactory downstreamSinkFactory;
    private Channel channel;
    private FilterFactory<ByteBuf> upstreamTransformFactory;
    private FilterFactory<ByteBuf> downstreamTransformFactory;

    ProxyChannel(
            final Endpoint endpoint,
            final UpstreamSslConfig sslConfig,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final DownstreamChannelFactory downstreamSinkFactory,
            final FilterFactory<ByteBuf> upstreamFilterFactory,
            final FilterFactory<ByteBuf> downstreamFilterFactory
    ) {
        this.endpoint = endpoint;
        this.sslConfig = sslConfig;
        this.bossGroup = bossGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.downstreamSinkFactory = downstreamSinkFactory;
        this.upstreamTransformFactory = upstreamFilterFactory;
        this.downstreamTransformFactory = downstreamFilterFactory;
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

    private ChannelInitializer<SocketChannel> createProxyInitializer(final UpstreamSslConfig upstreamSslConfig) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                LOG.trace("Incoming connection: {}", ch.remoteAddress());
                pipeline.addLast("ssl", createSslHandler(upstreamSslConfig));
                pipeline.addLast(createDownstreamHandler(new SocketChannelSink(ch)));
            }
        };
    }

    private ForwardingInboundHandler createDownstreamHandler(final Sink<ByteBuf> upstreamSink) {
        return new ForwardingInboundHandler(certSupplier -> {
            try {
                return downstreamSinkFactory.create(
                        certSupplier,
                        upstreamSink,
                        upstreamTransformFactory,
                        downstreamTransformFactory
                );
            } catch (final RuntimeException e) {
                LOG.error("Failed to create downstream channel", e);
                throw e;
            }
        });
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
