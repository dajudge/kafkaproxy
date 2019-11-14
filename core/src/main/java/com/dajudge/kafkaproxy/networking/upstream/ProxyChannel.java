package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory.UpstreamCertificateSupplier;
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
    private final Channel channel;

    public ProxyChannel(
            final int port,
            final ProxySslConfig proxySslConfig,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final ForwardChannelFactory forwardChannelFactory
    ) {
        try {
            final ChannelInitializer<SocketChannel> childHandler = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) {
                    final ChannelPipeline pipeline = ch.pipeline();
                    LOG.trace("Incoming connection: {}", ch.remoteAddress());
                    final Consumer<ByteBuf> upstreamSink = buffer -> {
                        ch.writeAndFlush(buffer.copy()).addListener((ChannelFutureListener) future -> {
                            buffer.release();
                            if (!future.isSuccess()) {
                                LOG.error("Failed to send {} bytes upstream.", buffer.readableBytes(), future.cause());
                            } else {
                                LOG.trace("Sent {} bytes upstream.", buffer.readableBytes());
                            }
                        });
                    };

                    pipeline.addLast("ssl", ProxySslHandlerFactory.createHandler(proxySslConfig));
                    final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> downstreamFactory = certSupplier -> {
                        final Runnable downstreamClosedCallback = () -> {
                            LOG.trace("Closing upstream channel.");
                            try {
                                ch.close().sync();
                            } catch (final InterruptedException e) {
                                LOG.warn("Cloud not close upstream channel.", e);
                            }
                            LOG.trace("Upstream channel closed.");
                        };
                        final ForwardChannel forwardChannel = forwardChannelFactory.create(
                                certSupplier,
                                upstreamSink,
                                downstreamClosedCallback
                        );
                        ch.closeFuture().addListener((ChannelFutureListener) future -> forwardChannel.close());
                        return forwardChannel::accept;
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
        return ((InetSocketAddress)channel.localAddress()).getPort();
    }
}
