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

package com.dajudge.kafkaproxy.common.ssl;

import com.dajudge.kafkaproxy.config.FileResource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class DefaultKeyManagerFactory {
    public static KeyManager[] createKeyManagers(
            final FileResource keyStore,
            final char[] keyStorePassword,
            final char[] keyPassword
    ) {
        try (final InputStream inputStream = keyStore.open()) {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(inputStream, keyStorePassword);
            factory.init(keystore, keyPassword);
            return factory.getKeyManagers();
        } catch (final UnrecoverableKeyException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to setup key manager", e);
        }
    }
}
