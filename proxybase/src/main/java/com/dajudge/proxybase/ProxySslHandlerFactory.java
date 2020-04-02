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

import com.dajudge.proxybase.config.UpstreamConfig;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.dajudge.proxybase.DefaultKeyManagerFactory.createKeyManagers;
import static com.dajudge.proxybase.DefaultTrustManagerFactory.createTrustManagers;

class ProxySslHandlerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ProxySslHandlerFactory.class);

    static ChannelHandler createSslHandler(final UpstreamConfig config) {
        return config.isEnabled()
                ? createHandlerInternal(config)
                : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(final UpstreamConfig config) {
        LOG.info("Creating proxy channel SSL handler");
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
            engine.setNeedClientAuth(config.isClientAuthRequired());
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize upstream SSL handler", e);
        }
    }
}
