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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

class ProxyClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientHandler.class);
    private final String channelId;
    private final Consumer<ByteBuf> messageSink;

    ProxyClientHandler(
            final String channelId,
            final Consumer<ByteBuf> messageSink
    ) {
        this.channelId = channelId;
        this.messageSink = messageSink;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LogHelper.withChannelId(channelId, () -> {
            final ByteBuf m = (ByteBuf) msg;
            LOG.trace("Received {} bytes from downstream.", m.readableBytes());
            try {
                messageSink.accept(m);
            } catch (final ProxyInternalException e) {
                LOG.error("Internal proxy error processing message from downstream. Killing channel.", e);
                ctx.close();
            } catch (final Exception e) {
                LOG.debug("Exception prcessing message from downstrean. Killing channel.", e);
                ctx.close();
            } finally {
                m.release();
            }
        });
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) {
        LOG.trace("Channel registered.");
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        LOG.trace("Channel unregistered.");
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LogHelper.withChannelId(channelId, () -> {
            LOG.debug("Uncaught exception processing message from downstream. Killing channel.", cause);
            ctx.close();
        });
    }
}
