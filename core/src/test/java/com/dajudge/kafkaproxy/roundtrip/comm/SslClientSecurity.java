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

package com.dajudge.kafkaproxy.roundtrip.comm;

import com.dajudge.kafkaproxy.roundtrip.ssl.KeyStoreWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import static java.io.File.createTempFile;

public class SslClientSecurity implements ClientSecurity {
    private final File trustStoreFile;
    private final Optional<Function<String, KeyStoreWrapper>> keyStoreFactory;
    private final KeyStoreWrapper trustStore;
    private final String proxyCertStrategy;

    public SslClientSecurity(
            final KeyStoreWrapper trustStore,
            final Optional<Function<String, KeyStoreWrapper>> keyStoreFactory,
            final String proxyCertStrategy
    ) {
        this.trustStore = trustStore;
        trustStoreFile = writeToTemp(trustStore);
        this.keyStoreFactory = keyStoreFactory;
        this.proxyCertStrategy = proxyCertStrategy;
    }

    private static File writeToTemp(final KeyStoreWrapper trustStore) {
        try {
            File keyStoreFile = createTempFile("kafkaproxy-test-", ".jks");
            keyStoreFile.deleteOnExit();
            try (final FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
                fos.write(trustStore.getBytes());
            }
            return keyStoreFile;
        } catch (final IOException e) {
            throw new RuntimeException("Failed to write keystore to /tmp", e);
        }
    }

    @Override
    public String getProtocol() {
        return "SSL";
    }


    @Override
    public String getTrustStoreLocation() {
        return trustStoreFile.getAbsolutePath();
    }

    @Override
    public String getTrustStorePassword() {
        return trustStore.getKeyStorePassword();
    }

    @Override
    public ClientSslConfig newClient(final String dn) {
        final Optional<KeyStoreWrapper> keyStore = keyStoreFactory.map(f -> f.apply(dn));
        final Optional<File> keyStoreFile = keyStore.map(SslClientSecurity::writeToTemp);
        return new ClientSslConfig() {
            @Override
            public String getKeyStoreLocation() {
                return keyStoreFile.map(File::getAbsolutePath).orElse(null);
            }

            @Override
            public String getKeyStorePassword() {
                return keyStore.map(KeyStoreWrapper::getKeyStorePassword).orElse(null);
            }

            @Override
            public String getKeyPassword() {
                return keyStore.map(KeyStoreWrapper::getKeyPassword).orElse(null);
            }

            @Override
            public String getProxyCertStrategy() {
                return proxyCertStrategy;
            }
        };
    }
}
