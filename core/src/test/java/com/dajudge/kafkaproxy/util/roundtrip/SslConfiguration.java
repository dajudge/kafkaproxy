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

package com.dajudge.kafkaproxy.util.roundtrip;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

public interface SslConfiguration {
    SslTestSetup createSslSetup();

    TestEnvironment applyProxyConfig(
            TestEnvironment environment,
            SslTestSetup sslSetup
    );

    void applyClientConfig(
            Map<String, Object> config,
            SslTestSetup sslTestSetup
    );

    <T extends GenericContainer<T>> GenericContainer<T> applyKafkaConfig(
            final String brokerName,
            final GenericContainer<T> container,
            final SslTestSetup sslSetup
    );

    SslConfiguration NONE = new SslConfiguration() {
        @Override
        public SslTestSetup createSslSetup() {
            return null;
        }

        @Override
        public TestEnvironment applyProxyConfig(
                final TestEnvironment environment,
                final SslTestSetup sslSetup
        ) {
            return environment;
        }

        @Override
        public void applyClientConfig(
                final Map<String, Object> config,
                final SslTestSetup sslTestSetup
        ) {
        }

        @Override
        public <T extends GenericContainer<T>> GenericContainer<T> applyKafkaConfig(
                final String brokerName,
                final GenericContainer<T> container,
                final SslTestSetup sslSetup
        ) {
            return container;
        }
    };

}
