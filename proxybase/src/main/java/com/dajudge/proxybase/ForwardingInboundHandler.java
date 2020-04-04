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

import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import static com.dajudge.proxybase.LogHelper.withChannelId;

class ForwardingInboundHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardingInboundHandler.class);
    private final String channelId;
    private final Function<UpstreamCertificateSupplier, Sink<ByteBuf>> sinkFactory;
    private Sink<ByteBuf> sink;

    ForwardingInboundHandler(
            final String channelId,
            final Function<UpstreamCertificateSupplier, Sink<ByteBuf>> sinkFactory
    ) {
        this.channelId = channelId;
        this.sinkFactory = sinkFactory;
    }

    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) {
        final UpstreamCertificateSupplier certSupplier = () -> {
            final ChannelHandler sslHandler = ctx.channel().pipeline().get("ssl");
            if (sslHandler instanceof SslHandler) {
                final SSLSession session = ((SslHandler) sslHandler).engine().getSession();
                final Certificate[] clientCerts = session.getPeerCertificates();
                return (X509Certificate) clientCerts[0];
            } else {
                throw new SSLPeerUnverifiedException("Upstream SSL not enabled");
            }
        };
        sink = sinkFactory.apply(certSupplier);
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        if (sink != null) {
            sink.close();
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        withChannelId(channelId, () -> {
            final ByteBuf buffer = ((ByteBuf) msg);
            LOG.trace("Received {} bytes from upstream.", buffer.readableBytes());
            try {
                sink.accept(buffer);
            } catch (final ProxyInternalException e) {
                LOG.error("Internal proxy error processing message from upstream. Killing channel.", e);
                ctx.close();
            } catch (final Exception e) {
                LOG.debug("Exception prcessing message from upstream. Killing channel.", e);
                ctx.close();
            } finally {
                buffer.release();
            }
        });
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        withChannelId(channelId, () -> {
            LOG.error("Internal proxy error processing message from upstream. Killing channel.", cause);
            ctx.close();
        });
    }

}