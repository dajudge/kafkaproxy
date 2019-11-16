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

package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.config.FileResource;

public class ProxySslConfig {
    public static final ProxySslConfig DISABLED = new ProxySslConfig(false, null, null, null, null, null);

    private final boolean enabled;
    private final FileResource trustStore;
    private final String trustStorePassword;
    private final FileResource keyStore;
    private final String keyStorePassword;
    private final String keyPassword;

    public ProxySslConfig(
            final boolean enabled,
            final FileResource trustStore,
            final String trustStorePassword,
            final FileResource keyStore,
            final String keyStorePassword,
            final String keyPassword
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public FileResource getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public FileResource getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
