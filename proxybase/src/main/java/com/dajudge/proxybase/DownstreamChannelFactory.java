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

package com.dajudge.proxybase;

import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;

class DownstreamChannelFactory {
    private final Endpoint endpoint;
    private final DownstreamSslConfig sslConfig;
    private final EventLoopGroup downstreamWorkerGroup;
    private final ClientCertificateAuthority clientCertificateAuthority;

    DownstreamChannelFactory(
            final Endpoint endpoint,
            final DownstreamSslConfig sslConfig,
            final EventLoopGroup downstreamWorkerGroup,
            final ClientCertificateAuthority clientCertificateAuthority
    ) {
        this.endpoint = endpoint;
        this.sslConfig = sslConfig;
        this.downstreamWorkerGroup = downstreamWorkerGroup;
        this.clientCertificateAuthority = clientCertificateAuthority;
    }

    Sink<ByteBuf> create(
            final UpstreamCertificateSupplier certificateSupplier,
            final Sink<ByteBuf> upstreamSink,
            final FilterFactory<ByteBuf> upstreamFilterFactory,
            final FilterFactory<ByteBuf> downstreamFilterFactory
    ) {
        return downstreamFilterFactory.apply(new DownstreamClient(
                endpoint,
                sslConfig,
                upstreamFilterFactory.apply(upstreamSink),
                downstreamWorkerGroup,
                clientCertificateAuthority.apply(certificateSupplier)
        ));
    }
}
