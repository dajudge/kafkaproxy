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

package com.dajudge.kafkaproxy.util.certs;

import com.dajudge.kafkaproxy.ca.Helpers;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class CertificateAuthority {
    private final KeyPair caKeyPair;
    private final X509Certificate caCert;

    public CertificateAuthority(final KeyPair caKeyPair, final X509Certificate caCert) {
        this.caKeyPair = caKeyPair;
        this.caCert = caCert;
    }

    public static CertificateAuthority create(final String dn) {
        final KeyPair keyPair = Helpers.keyPair();
        final X509Certificate cert = Helpers.selfSignedCert(dn, keyPair, 10, "SHA256withRSA");
        return new CertificateAuthority(keyPair, cert);
    }

    public SignedKeyPair createAndSignKeyPair(final String dn) {
        return signKeyPair(dn, Helpers.keyPair());
    }

    public SignedKeyPair signKeyPair(final String dn, final KeyPair keyPair) {
        final X509Certificate cert = Helpers.sign(dn, dn(), caKeyPair, 10, "SHA256withRSA", keyPair.getPublic());
        return new SignedKeyPair(keyPair, cert);
    }

    private String dn() {
        return caCert.getSubjectDN().getName();
    }

    public byte[] toTrustStore(final String password) {
        return Helpers.createJks(password, keystore -> {
            keystore.setCertificateEntry("ca", caCert);
        });
    }

}
