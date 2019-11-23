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

package com.dajudge.kafkaproxy.util.roundtrip.client.twowayssl;

import com.dajudge.kafkaproxy.util.roundtrip.ClientConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeyStore;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Map;

public class ClientTwoWaySslClientConfgurator implements ClientConfigurator {
    private final SslTestKeyStore keyStore;

    public ClientTwoWaySslClientConfgurator(final SslTestKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public void apply(final Map<String, Object> config) {
        config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStore.getKeyStore().getAbsolutePath());
        config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStore.getKeyStorePassword());
        config.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyStore.getKeyPassword());
    }
}
