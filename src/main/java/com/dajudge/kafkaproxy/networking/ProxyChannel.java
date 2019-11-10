package com.dajudge.kafkaproxy.networking;

import com.dajudge.kafkaproxy.brokermap.BrokerMapper;
import com.dajudge.kafkaproxy.protocol.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ProxyChannel {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannel.class);
    private final Channel channel;

    public ProxyChannel(
            final int port,
            final String kafkaHost,
            final int kafkaPort,
            final BrokerMapper brokerMapper,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final NioEventLoopGroup downstreamWorkerGroup
    ) {
        try {
            final ChannelInitializer<SocketChannel> childHandler = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) {
                    LOG.trace("Incoming connection: {}", ch.remoteAddress());
                    final Consumer<ByteBuf> sink = buffer -> {
                        ch.writeAndFlush(buffer.copy()).addListener((ChannelFutureListener) future -> {
                            buffer.release();
                            if (!future.isSuccess()) {
                                LOG.error("Failed to send {} bytes upstream.", buffer.readableBytes(), future.cause());
                            } else {
                                LOG.trace("Sent {} bytes upstream.", buffer.readableBytes());
                            }
                        });
                    };
                    final ResponseRewriter rewriter = new MetadataRewriter(brokerMapper);
                    final KafkaRequestStore requestStore = new KafkaRequestStore(rewriter);
                    final KafkaResponseProcessor responseProcessor = new KafkaResponseProcessor(sink, requestStore);
                    final KafkaMessageSplitter responseStreamSplitter = new KafkaMessageSplitter(
                            responseProcessor::onResponse
                    );
                    final DownstreamClient downstreamClient = new DownstreamClient(
                            kafkaHost,
                            kafkaPort,
                            responseStreamSplitter::onBytesReceived,
                            () -> {
                                LOG.trace("Closing upstream channel.");
                                try {
                                    ch.close().sync();
                                } catch (final InterruptedException e) {
                                    LOG.warn("Cloud not close upstream channel.", e);
                                }
                                LOG.trace("Upstream channel closed.");
                            },
                            downstreamWorkerGroup
                    );
                    ch.closeFuture().addListener(future -> {
                        downstreamClient.close();
                    });
                    final KafkaRequestProcessor requestProcessor = new KafkaRequestProcessor(
                            downstreamClient::send,
                            requestStore
                    );
                    final KafkaMessageSplitter splitter = new KafkaMessageSplitter(requestProcessor::onRequest);
                    ch.pipeline().addLast(new ProxyServerHandler(splitter::onBytesReceived));
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
}
