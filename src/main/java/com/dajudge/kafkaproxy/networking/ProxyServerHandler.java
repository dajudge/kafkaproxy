package com.dajudge.kafkaproxy.networking;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerHandler.class);
    private final Consumer<ByteBuf> sink;

    ProxyServerHandler(final Consumer<ByteBuf> sink) {
        this.sink = sink;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final ByteBuf buffer = ((ByteBuf) msg);
        LOG.trace("Received {} bytes from upstream.", buffer.readableBytes());
        try {
            sink.accept(buffer);
        } finally {
            buffer.release();
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        LOG.trace("Read from upstream complete.");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.debug("Exception caught in upstream channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

}