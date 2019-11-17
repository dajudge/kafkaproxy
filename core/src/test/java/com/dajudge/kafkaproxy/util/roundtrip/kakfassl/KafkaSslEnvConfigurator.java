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

package com.dajudge.kafkaproxy.util.roundtrip.kakfassl;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.roundtrip.EnvConfigurator;
import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;

public class KafkaSslEnvConfigurator implements EnvConfigurator {
    private final SslTestSetup sslSetup;

    public KafkaSslEnvConfigurator(final SslTestSetup sslSetup) {
        this.sslSetup = sslSetup;
    }

    @Override
    public TestEnvironment configure(final TestEnvironment e) {
        final SslTestAuthority kafkaAuthority = sslSetup.getAuthority();
        return e.withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", "kafka/truststore.jks")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", kafkaAuthority.getTrustStorePassword())
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true")
                .withFile("kafka/truststore.jks", kafkaAuthority.getTrustStore());
    }
}
