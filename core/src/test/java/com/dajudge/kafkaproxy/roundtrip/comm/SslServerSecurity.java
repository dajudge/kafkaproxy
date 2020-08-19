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

import com.dajudge.kafkaproxy.roundtrip.ssl.KeyStoreData;
import org.testcontainers.images.builder.Transferable;

import java.util.function.BiConsumer;

public class SslServerSecurity implements ServerSecurity {
    private static final String KEYSTORE_LOCATION = "/tmp/keystore.jks";
    private static final String TRUSTSTORE_LOCATION = "/tmp/truststore.jks";

    private final KeyStoreData keyStore;
    private final KeyStoreData trustStore;
    private final boolean requireClientAuth;

    public SslServerSecurity(
            final KeyStoreData keyStore,
            final KeyStoreData trustStore,
            final boolean requireClientAuth
    ) {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.requireClientAuth = requireClientAuth;
    }

    @Override
    public String getClientProtocol() {
        return "SSL";
    }

    @Override
    public String getTrustStoreLocation() {
        return TRUSTSTORE_LOCATION;
    }

    @Override
    public char[] getTrustStorePassword() {
        return trustStore.getKeyStorePassword();
    }

    @Override
    public String getTrustStoreType() {
        return trustStore.getType();
    }

    @Override
    public String getKeyStoreLocation() {
        return KEYSTORE_LOCATION;
    }

    @Override
    public char[] getKeyStorePassword() {
        return keyStore.getKeyStorePassword();
    }

    @Override
    public String getKeyStoreType() {
        return keyStore.getType();
    }

    @Override
    public char[] getKeyPassword() {
        return keyStore.getKeyPassword();
    }

    @Override
    public String getClientAuth() {
        return requireClientAuth ? "required" : "none";
    }

    @Override
    public byte[] getKeyStore() {
        return keyStore.getBytes();
    }

    @Override
    public byte[] getTrustStore() {
        return trustStore.getBytes();
    }

    @Override
    public void uploadKeyStores(final BiConsumer<Transferable, String> uploader) {
        uploader.accept(Transferable.of(keyStore.getBytes()), KEYSTORE_LOCATION);
        uploader.accept(Transferable.of(trustStore.getBytes()), TRUSTSTORE_LOCATION);
    }
}
