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

import com.dajudge.kafkaproxy.itest.util.KafkaProxyContainer;
import com.dajudge.kafkaproxy.roundtrip.util.PortFinder;
import org.junit.ClassRule;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.testcontainers.containers.KafkaContainer.KAFKA_PORT;

public class BaseIntegrationTest {
    private static final String CONFLUENT_PLATFORM_VERSION = System.getenv("CONFLUENT_PLATFORM_VERSION");
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:" + CONFLUENT_PLATFORM_VERSION;
    @ClassRule
    public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    public <T> T withKafkaProxy(final ThrowingFunction<String, T> runnable) {
        try (final ProxyContainer proxy = createKafkaProxy(KAFKA.getHost(), KAFKA.getMappedPort(KAFKA_PORT))) {
            return runnable.apply(proxy.getProxyEndpoint());
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    private ProxyContainer createKafkaProxy(final String kafkaHost, final int kafkaPort) {
        final KafkaProxyPorts ports = findFreePorts();
        final KafkaProxyContainer container = new KafkaProxyContainer()
                .withEnv("KAFKAPROXY_BOOTSTRAP_SERVERS", format("%s:%s", kafkaHost, kafkaPort))
                .withEnv("KAFKAPROXY_BASE_PORT", valueOf(ports.proxyPort))
                .withEnv("KAFKAPROXY_HTTP_PORT", valueOf(ports.httpPort))
                .withEnv("KAFKAPROXY_HOSTNAME", "localhost");
        container.start();
        return new ProxyContainer(container, format("localhost:%d", ports.proxyPort));
    }

    private KafkaProxyPorts findFreePorts() {
        try (final PortFinder portFinder = new PortFinder()) {
            return new KafkaProxyPorts(portFinder.nextPort(), portFinder.nextPort());
        }
    }

    private static class KafkaProxyPorts {
        private final int proxyPort;
        private final int httpPort;

        private KafkaProxyPorts(final int proxyPort, final int httpPort) {
            this.proxyPort = proxyPort;
            this.httpPort = httpPort;
        }
    }

    private static class ProxyContainer implements AutoCloseable {
        private final KafkaProxyContainer container;
        private final String proxyEndpoint;

        private ProxyContainer(final KafkaProxyContainer container, final String proxyEndpoint) {
            this.container = container;
            this.proxyEndpoint = proxyEndpoint;
        }

        @Override
        public void close() {
            container.close();
        }

        public String getProxyEndpoint() {
            return proxyEndpoint;
        }
    }

    public interface ThrowingFunction<I,O> {
        O apply(I t) throws Exception;
    }
}
