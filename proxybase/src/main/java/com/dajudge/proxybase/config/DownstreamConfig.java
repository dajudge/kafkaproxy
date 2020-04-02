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

package com.dajudge.proxybase.config;

import java.io.InputStream;
import java.util.function.Supplier;

public class DownstreamConfig {
    private final boolean enabled;
    private final Supplier<InputStream> trustStore;
    private final String trustStorePassword;
    private final boolean hostnameVerificationEnabled;

    public static final DownstreamConfig DISABLED = new DownstreamConfig(
            false,
            null,
            null,
            false
    );

    public DownstreamConfig(
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
