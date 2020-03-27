/*
 * Copyright 2019-2020 Alex Stockinger
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

import com.dajudge.kafkaproxy.ca.UpstreamCertificateSupplier;
import com.dajudge.kafkaproxy.networking.FilterFactory;
import com.dajudge.kafkaproxy.networking.upstream.DownstreamSinkFactory;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;

public class DownstreamChannelFactory implements DownstreamSinkFactory {
    private final String downstreamHostname;
    private final int downstreamPort;
    private final DownstreamSslConfig sslConfig;
    private final EventLoopGroup downstreamWorkerGroup;
    private final ClientCertificateAuthority clientCertificateAuthority;

    public DownstreamChannelFactory(
            final String downstreamHostname,
            final int downstreamPort,
            final DownstreamSslConfig sslConfig,
            final EventLoopGroup downstreamWorkerGroup,
            final ClientCertificateAuthority clientCertificateAuthority
    ) {
        this.downstreamHostname = downstreamHostname;
        this.downstreamPort = downstreamPort;
        this.sslConfig = sslConfig;
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.clientCertificateAuthority = clientCertificateAuthority;
    }

    @Override
    public ForwardChannel<ByteBuf> create(
            final UpstreamCertificateSupplier certificateSupplier,
            final ForwardChannel<ByteBuf> upstreamSink,
            final FilterFactory<ByteBuf> upstreamFilterFactory,
            final FilterFactory<ByteBuf> downstreamFilterFactory
    ) {
        return downstreamFilterFactory.apply(new DownstreamClient(
                downstreamHostname,
                downstreamPort,
                sslConfig,
                upstreamFilterFactory.apply(upstreamSink),
                downstreamWorkerGroup,
                clientCertificateAuthority.apply(certificateSupplier)
        ));
    }
}
