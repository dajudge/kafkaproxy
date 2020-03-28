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

package com.dajudge.proxybase.config;

import java.io.InputStream;
import java.util.function.Supplier;

public class DownstreamSslConfig {
    private final boolean enabled;
    private final Supplier<InputStream> trustStore;
    private final String trustStorePassword;
    private final boolean hostnameVerificationEnabled;

    public static final DownstreamSslConfig DISABLED = new DownstreamSslConfig(
            false,
            null,
            null,
            false
    );

    public DownstreamSslConfig(
            final boolean enabled,
            final Supplier<InputStream> trustStore,
            final String trustStorePassword,
            final boolean hostnameVerificationEnabled
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    public Supplier<InputStream> getTrustStore() {
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
}
