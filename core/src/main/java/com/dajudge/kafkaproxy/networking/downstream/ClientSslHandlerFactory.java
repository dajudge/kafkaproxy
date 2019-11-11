package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.common.ssl.DefaultTrustManagerFactory;
import com.dajudge.kafkaproxy.common.ssl.NullChannelHandler;
import com.dajudge.kafkaproxy.networking.trustmanager.HostCheckingTrustManager;
import com.dajudge.kafkaproxy.networking.trustmanager.HostnameCheck;
import com.dajudge.kafkaproxy.networking.trustmanager.HttpClientHostnameCheck;
import io.netty.channel.ChannelHandler;
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

public class ClientSslHandlerFactory {
    public static ChannelHandler createHandler(final KafkaSslConfig config, final String kafkaHostname, final int port) {
        return config.isEnabled() ? createHandlerInternal(config, kafkaHostname, port) : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(final KafkaSslConfig config, final String kafkaHostname, final int port) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final HostnameCheck hostnameCheck = config.isHostnameVerificationEnabled()
                    ? new HttpClientHostnameCheck(kafkaHostname)
                    : HostnameCheck.NULL_VERIFIER;
            final TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(createDefaultTrustManagers(config), hostnameCheck)
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
        return Stream.of((DefaultTrustManagerFactory.createTrustManagers(
                kafkaSslConfig.getTrustStore(),
                kafkaSslConfig.getTrustStorePassword().toCharArray()
        ))).map(it -> (X509TrustManager) it).collect(toList());
    }
}
