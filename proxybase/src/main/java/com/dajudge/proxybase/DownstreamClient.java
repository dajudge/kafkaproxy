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

import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.config.DownstreamConfig;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.dajudge.proxybase.ClientSslHandlerFactory.createHandler;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;


class DownstreamClient implements Sink<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamClient.class);
    private final Channel channel;

    DownstreamClient(
            final Endpoint endpoint,
            final DownstreamConfig sslConfig,
            final Sink<ByteBuf> messageSink,
            final EventLoopGroup workerGroup,
            final KeyStoreWrapper keyStore
    ) {
        final ChannelHandler sslHandler = createHandler(sslConfig, endpoint, keyStore);
        try {
            channel = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(sslHandler);
                            pipeline.addLast(new ProxyClientHandler(messageSink));
                        }
                    })
                    .connect(endpoint.getHost(), endpoint.getPort()).sync().channel();
            LOG.trace("Downstream channel established: {}", endpoint);
            channel.closeFuture().addListener(future -> {
                LOG.trace("Downstream channel closed: {}", endpoint);
                messageSink.close();
            });
            LOG.trace("Downstream connection established to {}", endpoint);
        } catch (final InterruptedException e) {
            LOG.debug("Failed to establish downstream connection to {}", endpoint, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChannelFuture close() {
        return channel.close();
    }

    @Override
    public void accept(final ByteBuf buffer) {
        LOG.trace("Sending {} bytes downstream.", buffer.readableBytes());
        channel.writeAndFlush(buffer).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                LOG.debug("Failed to send {} bytes downstream.", buffer.readableBytes(), future.cause());
            } else {
                LOG.trace("Sent {} bytes downstream.", buffer.readableBytes());
            }
        });
    }
}
