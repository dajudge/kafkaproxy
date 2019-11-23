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

package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.ca.ProxyClientCertificateAuthorityFactory.CertificateAuthority;
import com.dajudge.kafkaproxy.ca.UpstreamCertificateSupplier;
import com.dajudge.kafkaproxy.common.ssl.DefaultTrustManagerFactory;
import com.dajudge.kafkaproxy.common.ssl.NullChannelHandler;
import com.dajudge.kafkaproxy.networking.trustmanager.HostCheckingTrustManager;
import com.dajudge.kafkaproxy.networking.trustmanager.HostnameCheck;
import com.dajudge.kafkaproxy.networking.trustmanager.HttpClientHostnameCheck;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.security.*;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ClientSslHandlerFactory {

    public static ChannelHandler createHandler(
            final KafkaSslConfig config,
            final String kafkaHostname,
            final int port,
            final UpstreamCertificateSupplier certSupplier,
            final CertificateAuthority clientCertFactory,
            final String keyPassword
    ) {
        return config.isEnabled()
                ? createHandlerInternal(config, kafkaHostname, port, certSupplier, clientCertFactory, keyPassword)
                : new NullChannelHandler();
    }

    private static ChannelHandler createHandlerInternal(
            final KafkaSslConfig config,
            final String kafkaHostname,
            final int port,
            final UpstreamCertificateSupplier certificateSupplier,
            final CertificateAuthority clientCertFactory,
            final String keyPassword
    ) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final KeyStore keyStore = clientCertFactory.createClientCertificate(certificateSupplier);
            final HostnameCheck hostnameCheck = config.isHostnameVerificationEnabled()
                    ? new HttpClientHostnameCheck(kafkaHostname)
                    : HostnameCheck.NULL_VERIFIER;
            final TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(createDefaultTrustManagers(config), hostnameCheck)
            };
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
            );
            keyManagerFactory.init(keyStore, keyPassword.toCharArray());
            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine(kafkaHostname, port);
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException
                | KeyManagementException
                | SSLPeerUnverifiedException
                | KeyStoreException
                | UnrecoverableKeyException e
        ) {
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
