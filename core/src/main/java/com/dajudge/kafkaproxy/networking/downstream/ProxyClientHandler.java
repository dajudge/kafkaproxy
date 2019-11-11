package com.dajudge.kafkaproxy.networking.downstream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ProxyClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientHandler.class);
    private final Consumer<ByteBuf> messageSink;

    ProxyClientHandler(final Consumer<ByteBuf> messageSink) {
        this.messageSink = messageSink;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final ByteBuf m = (ByteBuf) msg;
        LOG.trace("Received {} bytes from downstream.", m.readableBytes());
        try {
            messageSink.accept(m);
        } finally {
            m.release();
        }
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        LOG.trace("Channel registered.");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        LOG.trace("Channel unregistered.");
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.debug("Exception caught in downstream channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
