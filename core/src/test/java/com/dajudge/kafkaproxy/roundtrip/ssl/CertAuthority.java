/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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


import com.dajudge.proxybase.ca.Helpers;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;

import static com.dajudge.kafkaproxy.roundtrip.util.Util.randomIdentifier;
import static com.dajudge.proxybase.ca.Helpers.createKeyStore;
import static com.dajudge.proxybase.ca.Helpers.serialize;
import static java.time.temporal.ChronoUnit.DAYS;

public class CertAuthority {
    private final KeyPair keyPair;
    private final X509Certificate cert;

    public CertAuthority(final String dn) {
        keyPair = Helpers.keyPair();
        final Date now = Helpers.now(System::currentTimeMillis);
        cert = Helpers.selfSignedCert(
                dn,
                keyPair,
                now,
                Helpers.plus(now, Duration.of(10, DAYS)),
                "SHA256withRSA",
                true
        );
    }

    public KeyStoreData createSignedKeyPair(final String dn, final String keystoreType) {
        final KeyPair newKeyPair = Helpers.keyPair();
        final Date now = Helpers.now(System::currentTimeMillis);
        final X509Certificate newCert = Helpers.sign(
                dn,
                cert.getSubjectDN().getName(),
                keyPair.getPrivate(),
                "SHA256withRSA",
                newKeyPair.getPublic(),
                now,
                Helpers.plus(now, Duration.of(10, DAYS)),
                false
        );
        final char[] keyStorePassword = randomIdentifier().toCharArray();
        final char[] keyPassword = randomIdentifier().toCharArray();
        final byte[] keyStore = keyStoreOf(newKeyPair, newCert, keyStorePassword, keyPassword, keystoreType);
        return new KeyStoreData(keyStore, keyStorePassword, keyPassword, keystoreType);
    }

    private byte[] keyStoreOf(
            final KeyPair keyPair,
            final X509Certificate cert,
            final char[] keyStorePassword,
            final char[] keyPassword,
            final String type
    ) {
        return serialize(createKeyStore(keyStore -> {
            keyStore.setKeyEntry("key", keyPair.getPrivate(), keyPassword, new Certificate[]{cert});
        }, type), keyStorePassword);
    }

    public KeyStoreData getTrustStore(final String type) {
        final char[] keyStorePassword = randomIdentifier().toCharArray();
        final byte[] keyStore = serialize(createKeyStore(keystore -> {
            keystore.setCertificateEntry("ca", cert);
        }, type), keyStorePassword);
        return new KeyStoreData(keyStore, keyStorePassword, null, type);
    }
}
