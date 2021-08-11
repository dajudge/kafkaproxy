/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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
package com.dajudge.kafkaproxy.itest;

import com.dajudge.kafkaproxy.KafkaProxyApplication;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.itest.util.NullFileSystem;
import com.dajudge.kafkaproxy.roundtrip.util.PortFinder;
import com.dajudge.kafkaproxy.roundtrip.util.TestEnvironment;
import org.junit.ClassRule;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.testcontainers.containers.KafkaContainer.KAFKA_PORT;

public class BaseIntegrationTest {
    @ClassRule
    public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    public <T> T withKafkaProxy(final ThrowingFunction<String, T> runnable) {
        try (final ProxyContainer proxy = createKafkaProxy(KAFKA.getHost(), KAFKA.getMappedPort(KAFKA_PORT))) {
            return runnable.apply(proxy.getProxyEndpoint());
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    private ProxyContainer createKafkaProxy(final String kafkaHost, final int kafkaPort) {
        final int proxyPort = findFreePort();
        final TestEnvironment environment = new TestEnvironment()
                .withEnv("KAFKAPROXY_BOOTSTRAP_SERVERS", format("%s:%s", kafkaHost, kafkaPort))
                .withEnv("KAFKAPROXY_BASE_PORT", valueOf(proxyPort))
                .withEnv("KAFKAPROXY_HOSTNAME", "localhost");
        final KafkaProxyApplication app = new KafkaProxyApplication(
                new ApplicationConfig(environment),
                System::currentTimeMillis,
                new NullFileSystem()
        );
        return new ProxyContainer(app, format("localhost:%d", proxyPort));
    }

    private int findFreePort() {
        try (final PortFinder portFinder = new PortFinder()) {
            return portFinder.nextPort();
        }
    }

    private static class ProxyContainer implements AutoCloseable {
        private final KafkaProxyApplication application;
        private final String proxyEndpoint;

        private ProxyContainer(final KafkaProxyApplication application, final String proxyEndpoint) {
            this.application = application;
            this.proxyEndpoint = proxyEndpoint;
        }

        @Override
        public void close() {
            application.close();
        }

        public String getProxyEndpoint() {
            return proxyEndpoint;
        }
    }

    public interface ThrowingFunction<I,O> {
        O apply(I t) throws Exception;
    }
}
