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

package com.dajudge.kafkaproxy.ca.selfsign;

import com.dajudge.kafkaproxy.config.FileResource;

public class SelfSignConfig {
    private final String keyPassword;
    private final String keyAlias;
    private final String issuerDn;
    private final String signatureAlgorithm;
    private final FileResource keyStore;
    private final String keyStorePassword;

    public SelfSignConfig(
            final String issuerDn,
            final FileResource keyStore,
            final String keyStorePassword,
            final String keyAlias,
            final String keyPassword,
            final String signatureAlgorithm
    ) {
        this.issuerDn = issuerDn;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.signatureAlgorithm = signatureAlgorithm;
    }


    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public FileResource getKeyStore() {
        return keyStore;
    }

    public char[] getKeyStorePassword() {
        return keyStorePassword.toCharArray();
    }

    public char[] getKeyPassword() {
        return keyPassword.toCharArray();
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getIssuerDn() {
        return issuerDn;
    }
}
