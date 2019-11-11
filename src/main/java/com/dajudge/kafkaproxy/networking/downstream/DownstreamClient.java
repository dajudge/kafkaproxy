package com.dajudge.kafkaproxy.networking.downstream;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static io.netty.channel.ChannelOption.SO_KEEPALIVE;


public class DownstreamClient {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamClient.class);
    private final Channel channel;

    public DownstreamClient(
            final String host,
            final int port,
            final KafkaSslConfig sslConfig,
            final Consumer<ByteBuf> messageSink,
            final Runnable closeCallback,
            final EventLoopGroup workerGroup
    ) {
        try {
            channel = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(ClientSslHandlerFactory.createHandler(sslConfig, host, port));
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
