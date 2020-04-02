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

package com.dajudge.proxybase.ca;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

public class ClientCertCertificateAuthority implements CertificateAuthority {
    private final KeyStoreWrapper wrapper;

    public ClientCertCertificateAuthority(final ClientCertificateConfig sslConfig) {
        try (final InputStream is = sslConfig.getKeyStore().get()) {
            final KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(is, sslConfig.getKeyStorePassword().toCharArray());
            wrapper = new KeyStoreWrapper(keyStore, sslConfig.getKeyPassword());
        } catch (final IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to load client key store", e);
        }
    }

    @Override
    public KeyStoreWrapper createClientCertificate(
            final UpstreamCertificateSupplier certificateSupplier
    ) {
        return wrapper;
    }

    public static class ClientCertificateConfig {
        private final Supplier<InputStream> keyStore;
        private final String keyStorePassword;
        private final String keyPassword;

        public static final ClientCertificateConfig DISABLED = new ClientCertificateConfig(
                null,
                null,
                null
        );

        public ClientCertificateConfig(
                final Supplier<InputStream> keyStore,
                final String keyStorePassword,
                final String keyPassword
        ) {
            this.keyStore = keyStore;
            this.keyStorePassword = keyStorePassword;
            this.keyPassword = keyPassword;
        }

        public Supplier<InputStream> getKeyStore() {
            return keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public String getKeyPassword() {
            return keyPassword;
        }
    }
}
