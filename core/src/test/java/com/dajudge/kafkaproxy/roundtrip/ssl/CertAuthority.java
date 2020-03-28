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

package com.dajudge.kafkaproxy.roundtrip.ssl;

import com.dajudge.kafkaproxy.ca.selfsign.Helpers;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static com.dajudge.kafkaproxy.ca.selfsign.Helpers.createJks;
import static com.dajudge.kafkaproxy.ca.selfsign.Helpers.keyPair;
import static com.dajudge.kafkaproxy.roundtrip.util.Util.randomIdentifier;

public class CertAuthority {
    private final KeyPair keyPair;
    private final X509Certificate cert;

    public CertAuthority(final String dn) {
        keyPair = keyPair();
        cert = Helpers.selfSignedCert(dn, keyPair, 10, "SHA256withRSA", true);
    }

    public KeyStoreWrapper createSignedKeyPair(final String dn) {
        final KeyPair newKeyPair = keyPair();
        final X509Certificate newCert = Helpers.sign(
                dn,
                cert.getSubjectDN().getName(),
                keyPair.getPrivate(),
                10,
                "SHA256withRSA",
                newKeyPair.getPublic(),
                false
        );
        final String keyStorePassword = randomIdentifier();
        final String keyPassword = randomIdentifier();
        final byte[] keyStore = keyStoreOf(newKeyPair, newCert, keyStorePassword, keyPassword);
        return new KeyStoreWrapper(keyStore, keyStorePassword, keyPassword);
    }

    private byte[] keyStoreOf(
            final KeyPair keyPair,
            final X509Certificate cert,
            final String keyStorePassword,
            final String keyPassword
    ) {
        return createJks(keyStorePassword, keyStore -> {
            keyStore.setKeyEntry("key", keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert});
        });
    }

    public KeyStoreWrapper getTrustStore() {
        final String keyStorePassword = randomIdentifier();
        final byte[] keyStore = createJks(keyStorePassword, keystore -> {
            keystore.setCertificateEntry("ca", cert);
        });
        return new KeyStoreWrapper(keyStore, keyStorePassword, null);
    }
}
