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

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

class DefaultTrustManagerFactory {
    static TrustManager[] createTrustManagers(
            final Supplier<InputStream> trustStore,
            final char[] trustStorePassword
    ) {
        try (final InputStream inputStream = trustStore.get()) {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(inputStream, trustStorePassword);
            factory.init(keystore);
            return factory.getTrustManagers();
        } catch (final NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new RuntimeException("Failed to setup trust manager", e);
        }
    }
}
