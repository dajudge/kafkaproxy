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

package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.ca.NullProxyClientCertificateAuthorityFactory;
import com.dajudge.kafkaproxy.config.FileResource;

public class KafkaSslConfig {
    private final boolean enabled;
    private final FileResource trustStore;
    private final String trustStorePassword;
    private final boolean hostnameVerificationEnabled;
    private final String certificateFactory;

    public static final KafkaSslConfig DISABLED = new KafkaSslConfig(
            false,
            null,
            null,
            false,
            NullProxyClientCertificateAuthorityFactory.NAME
    );

    public KafkaSslConfig(
            final boolean enabled,
            final FileResource trustStore,
            final String trustStorePassword,
            final boolean hostnameVerificationEnabled,
            final String certificateFactory
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
        this.certificateFactory = certificateFactory;
    }

    public FileResource getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHostnameVerificationEnabled() {
        return hostnameVerificationEnabled;
    }

    public String getCertificateFactory() {
        return certificateFactory;
    }
}
