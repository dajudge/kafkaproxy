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

package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.ca.UpstreamCertificateSupplier;
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
import java.util.function.Consumer;
import java.util.function.Function;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerHandler.class);
    private final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> sinkFactory;
    private Consumer<ByteBuf> sink;

    public ProxyServerHandler(final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> sinkFactory) {
        this.sinkFactory = sinkFactory;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final ByteBuf buffer = ((ByteBuf) msg);
        LOG.trace("Received {} bytes from upstream.", buffer.readableBytes());
        try {
            getOrCreateSink(ctx).accept(buffer);
        } finally {
            buffer.release();
        }
    }

    private synchronized Consumer<ByteBuf> getOrCreateSink(final ChannelHandlerContext ctx) {
        if (sink == null) {
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
        return sink;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.debug("Exception caught in upstream channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

}