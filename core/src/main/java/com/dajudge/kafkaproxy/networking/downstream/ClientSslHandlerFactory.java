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

import com.dajudge.kafkaproxy.ca.KeyStoreWrapper;
import com.dajudge.kafkaproxy.common.ssl.DefaultTrustManagerFactory;
import com.dajudge.kafkaproxy.common.ssl.NullChannelHandler;
import com.dajudge.kafkaproxy.networking.trustmanager.HostCheckingTrustManager;
import com.dajudge.kafkaproxy.networking.trustmanager.HostnameCheck;
import com.dajudge.kafkaproxy.networking.trustmanager.HttpClientHostnameCheck;
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

public class ClientSslHandlerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClientSslHandlerFactory.class);

    public static ChannelHandler createHandler(
            final DownstreamSslConfig config,
            final String kafkaHostname,
            final int port,
            final Supplier<KeyStoreWrapper> clientKeyStoreSupplier
    ) {
        return config.isEnabled()
                ? createHandlerInternal(config, kafkaHostname, port, clientKeyStoreSupplier)
                : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(
            final DownstreamSslConfig config,
            final String kafkaHostname,
            final int port,
            final Supplier<KeyStoreWrapper> clientKeyStoreSupplier
    ) {
        try {
            LOG.info("Creating client SSL handler for {}:{}", kafkaHostname, port);
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final KeyStoreWrapper keyStore = clientKeyStoreSupplier.get();
            final HostnameCheck hostnameCheck = config.isHostnameVerificationEnabled()
                    ? new HttpClientHostnameCheck(kafkaHostname)
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
            final SSLEngine engine = clientContext.createSSLEngine(kafkaHostname, port);
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
