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

package com.dajudge.kafkaproxy.ca;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.kafkassl.KafkaSslConfig;
import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.ClientCertificateAuthority;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import static com.dajudge.kafkaproxy.ca.ProxyClientCertificateAuthorityFactoryRegistry.createCertificateFactory;

public class ClientCertificateAuthorityImpl implements ClientCertificateAuthority {
    private final ApplicationConfig appConfig;

    public ClientCertificateAuthorityImpl(final ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public Supplier<KeyStoreWrapper> apply(final UpstreamCertificateSupplier certificateSupplier) {
        final KafkaSslConfig sslConfig = appConfig.get(KafkaSslConfig.class);
        switch (sslConfig.getClientCertificateStrategy()) {
            case KEYSTORE:
                return createKeyStoreSupplier(sslConfig);
            case CA:
                return createCaKeyStoreSupplier(sslConfig, certificateSupplier);
            case NONE:
                return emptyKeyStoreSupplier();
            default:
                throw new IllegalArgumentException("Unhandled client certificate strategy: "
                        + sslConfig.getClientCertificateStrategy());
        }
    }

    private Supplier<KeyStoreWrapper> emptyKeyStoreSupplier() {
        return () -> {
            try {
                final KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, null);
                return new KeyStoreWrapper(keyStore, "noPassword");
            } catch (final KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                throw new RuntimeException("Failed to create empty key store", e);
            }
        };
    }

    private Supplier<KeyStoreWrapper> createKeyStoreSupplier(final KafkaSslConfig sslConfig) {
        final DownstreamSslConfig downstreamSslConfig = sslConfig.getDownstreamSslConfig();
        try (final InputStream is = downstreamSslConfig.getKeyStore().get()) {
            final KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(is, downstreamSslConfig.getKeyStorePassword().toCharArray());
            final KeyStoreWrapper wrapper = new KeyStoreWrapper(keyStore, downstreamSslConfig.getKeyPassword());
            return () -> wrapper;
        } catch (final IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to load client key store", e);
        }
    }

    private Supplier<KeyStoreWrapper> createCaKeyStoreSupplier(
            final KafkaSslConfig sslConfig,
            final UpstreamCertificateSupplier certificateSupplier) {
        final CertificateAuthority clientAuthority = createCertificateFactory(
                sslConfig.getCertificateFactory(),
                appConfig
        );
        return () -> {
            try {
                return clientAuthority.createClientCertificate(certificateSupplier);
            } catch (final SSLPeerUnverifiedException e) {
                throw new RuntimeException("Client did not provide certificate", e);
            }
        };
    }
}
