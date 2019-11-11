package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.common.ssl.NullChannelHandler;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.dajudge.kafkaproxy.common.ssl.DefaultKeyManagerFactory.createKeyManagers;
import static com.dajudge.kafkaproxy.common.ssl.DefaultTrustManagerFactory.createTrustManagers;

public class ProxySslHandlerFactory {
    public static ChannelHandler createHandler(final ProxySslConfig config) {
        return config.isEnabled() ? createHandlerInternal(config) : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(final ProxySslConfig config) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final TrustManager[] trustManagers = createTrustManagers(
                    config.getTrustStore(),
                    config.getTrustStorePassword().toCharArray()
            );
            final KeyManager[] keyManagers = createKeyManagers(
                    config.getKeyStore(),
                    config.getKeyStorePassword().toCharArray(),
                    config.getKeyPassword().toCharArray()
            );
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine();
            engine.setUseClientMode(false);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize upstream SSL handler", e);
        }
    }

}
