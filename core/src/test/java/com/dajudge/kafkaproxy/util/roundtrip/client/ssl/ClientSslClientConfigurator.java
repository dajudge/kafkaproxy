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

package com.dajudge.kafkaproxy.util.roundtrip.client.ssl;

import com.dajudge.kafkaproxy.util.roundtrip.ClientConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Map;

public class ClientSslClientConfigurator implements ClientConfigurator {
    private final SslTestSetup sslSetup;

    public ClientSslClientConfigurator(final SslTestSetup sslSetup) {
        this.sslSetup = sslSetup;
    }

    @Override
    public void apply(final Map<String, Object> config) {
        final SslTestAuthority clientAuthority = sslSetup.getAuthority();
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientAuthority.getTrustStore().getAbsolutePath());
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, clientAuthority.getTrustStorePassword());
    }
}
