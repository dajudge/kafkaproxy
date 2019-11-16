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

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static com.dajudge.kafkaproxy.ca.Helpers.createJks;


public class SignedKeyPair {
    private final KeyPair keyPair;
    private final X509Certificate cert;

    public SignedKeyPair(final KeyPair keyPair, final X509Certificate cert) {
        this.keyPair = keyPair;
        this.cert = cert;
    }

    public byte[] toKeyStore(final String keystorePassword, final String keyPassword) {
        return createJks(keystorePassword, keystore -> {
            keystore.setKeyEntry("key", keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert});
        });
    }
}
