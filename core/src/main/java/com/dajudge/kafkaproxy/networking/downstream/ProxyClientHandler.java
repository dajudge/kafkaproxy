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
