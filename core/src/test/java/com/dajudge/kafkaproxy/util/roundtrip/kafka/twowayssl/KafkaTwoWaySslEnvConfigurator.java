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

package com.dajudge.kafkaproxy.util.roundtrip.kafka.twowayssl;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.roundtrip.EnvConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;

public class KafkaTwoWaySslEnvConfigurator implements EnvConfigurator {
    private final SslTestSetup testSetup;

    public KafkaTwoWaySslEnvConfigurator(final SslTestSetup testSetup) {
        this.testSetup = testSetup;
    }

    @Override
    public TestEnvironment configure(final TestEnvironment e) {
        return e.withEnv("KAFKAPROXY_SELFSIGN_ISSUERDN", testSetup.getCaName())
                .withEnv("KAFKAPROXY_SELFSIGN_KEYSTORE_LOCATION", "selfSign.jks")
                .withEnv("KAFKAPROXY_SELFSIGN_KEYSTORE_PASSWORD", testSetup.getAuthority().getKeyStore().getKeyStorePassword())
                .withEnv("KAFKAPROXY_SELFSIGN_KEY_PASSWORD", testSetup.getAuthority().getKeyStore().getKeyPassword())
                .withEnv("KAFKAPROXY_SELFSIGN_KEY_ALIAS", "caKey")
                .withFile("selfSign.jks", testSetup.getAuthority().getKeyStore().getKeyStore());
    }
}
