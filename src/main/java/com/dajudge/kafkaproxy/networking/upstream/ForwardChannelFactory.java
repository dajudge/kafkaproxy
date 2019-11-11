package com.dajudge.kafkaproxy.networking.upstream;

import io.netty.buffer.ByteBuf;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.util.function.Consumer;

public interface ForwardChannelFactory {
    interface UpstreamCertificateSupplier {
        Certificate get() throws SSLPeerUnverifiedException;
    }

    ForwardChannel create(
            final UpstreamCertificateSupplier certSupplier,
            final Consumer<ByteBuf> upstreamSink,
            final Runnable downstreamClosedCallback
    );
}
