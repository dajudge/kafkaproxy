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

import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class ClientSslHandlerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClientSslHandlerFactory.class);

    static ChannelHandler createHandler(
            final DownstreamSslConfig config,
            final Endpoint endpoint,
            final KeyStoreWrapper keyStore
    ) {
        return config.isEnabled()
                ? createHandlerInternal(config, endpoint, keyStore)
                : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(
            final DownstreamSslConfig config,
            final Endpoint endpoint,
            final KeyStoreWrapper keyStore
    ) {
        try {
            LOG.info("Creating client SSL handler for {}", endpoint);
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final HostnameCheck hostnameCheck = config.isHostnameVerificationEnabled()
                    ? new HttpClientHostnameCheck(endpoint.getHost())
                    : HostnameCheck.NULL_VERIFIER;
            final TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(createDefaultTrustManagers(config), hostnameCheck)
            };
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
            );
            if (keyStore != null) {
                keyManagerFactory.init(keyStore.getKeyStore(), keyStore.getKeyPassword().toCharArray());
            }
            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine(endpoint.getHost(), endpoint.getPort());
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException
                | KeyManagementException
                | KeyStoreException
                | UnrecoverableKeyException e
        ) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }

    private static List<X509TrustManager> createDefaultTrustManagers(final DownstreamSslConfig downstreamSslConfig) {
        return Stream.of((DefaultTrustManagerFactory.createTrustManagers(
                downstreamSslConfig.getTrustStore(),
                downstreamSslConfig.getTrustStorePassword().toCharArray()
        ))).map(it -> (X509TrustManager) it).collect(toList());
    }
}
