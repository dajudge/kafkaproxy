/*
 * Copyright 2019 Alex Stockinger
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

package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.ca.UpstreamCertificateSupplier;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

import static com.dajudge.kafkaproxy.ca.ProxyClientCertificateAuthorityFactoryRegistry.createCertificateFactory;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;


public class DownstreamClient {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamClient.class);
    private final Channel channel;

    public DownstreamClient(
            final String host,
            final int port,
            final ApplicationConfig appConfig,
            final Consumer<ByteBuf> messageSink,
            final Runnable closeCallback,
            final EventLoopGroup workerGroup,
            final UpstreamCertificateSupplier certificateSupplier
    ) {
        final KafkaSslConfig sslConfig = appConfig.get(KafkaSslConfig.class);
        final String keyPassword = UUID.randomUUID().toString();
        final ChannelHandler sslHandler = ClientSslHandlerFactory.createHandler(
                sslConfig,
                host,
                port,
                certificateSupplier,
                createCertificateFactory(sslConfig.getCertificateFactory(), appConfig, keyPassword),
                keyPassword
        );
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
                    .connect(host, port).sync().channel();
            LOG.trace("Downstream channel established: {}: {}", host, port);
            channel.closeFuture().addListener(future -> {
                LOG.trace("Downstream channel closed: {}:{}", host, port);
                closeCallback.run();
            });
            LOG.trace("Downstream connection established to {}:{}", host, port);
        } catch (final InterruptedException e) {
            LOG.debug("Failed to establish downstream connection to {}:{}", host, port, e);
            throw new RuntimeException(e);
        }
    }

    public ChannelFuture close() {
        return channel.close();
    }

    public void send(final ByteBuf buffer) {
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
