package com.dajudge.kafkaproxy.networking;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class SslHandlerFactory {
    public static ChannelHandler create(final KafkaSslConfig kafkaSslConfig, final String kafkaHostname, final int port) {
        if (!kafkaSslConfig.isSslEnabled()) {
            return new ChannelHandlerAdapter() {
            };
        }
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(
                            createDefaultTrustManagers(kafkaSslConfig),
                            kafkaHostname,
                            kafkaSslConfig.isHostnameVerificationEnabled()
                    )
            };
            clientContext.init(null, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine(kafkaHostname, port);
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }

    private static List<X509TrustManager> createDefaultTrustManagers(final KafkaSslConfig kafkaSslConfig) {
        return Stream.of((DownstreamTrustManager.createTrustManagers(
                kafkaSslConfig.getTrustStore(),
                kafkaSslConfig.getTrustStorePassword().toCharArray()
        ))).map(it -> (X509TrustManager) it).collect(toList());
    }
}
