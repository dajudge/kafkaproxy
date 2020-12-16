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

package com.dajudge.kafkaproxy;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class IntegrationTest {
    private static final String PREFIX = IntegrationTest.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);
    private static final String TEST_TAG = System.getProperty(PREFIX + ".dockerImageTag", "latest");
    private static final String PROXY_TAG = System.getProperty(PREFIX + ".proxyImageTag", "latest");
    private static KafkaContainer kafkaContainer;
    private static ProxyContainer proxyContainer;

    @BeforeAll
    public static void createKafkaContainer() {
        kafkaContainer = new KafkaContainer() {
            public String getBootstrapServers() {
                super.getBootstrapServers();
                return String.format("PLAINTEXT://%s:%s", "kafka", KAFKA_PORT);
            }
        }
                .withNetworkAliases("kafka");
        kafkaContainer.setLogConsumers(singletonList(new Slf4jLogConsumer(LOG)));
        kafkaContainer.start();

        proxyContainer = new ProxyContainer(
                PROXY_TAG,
                kafkaContainer.getNetwork(),
                "proxy",
                kafkaContainer.getBootstrapServers().replaceAll("^[A-Z]+://", "")
        );
        proxyContainer.setLogConsumers(singletonList(new Slf4jLogConsumer(LOG)));
        proxyContainer.start();
    }

    @AfterAll
    public static void shutdownKafkaContainer() {
        try {
            if(proxyContainer != null) {
                proxyContainer.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        try {
            if(kafkaContainer != null) {
                kafkaContainer.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @TestFactory
    public Stream<DynamicTest> createTests() {
        return Stream.of("java", "dotnet").map(this::createDynamicTest);
    }

    private DynamicTest createDynamicTest(final String imageName) {
        return dynamicTest("Integration test \"" + imageName + "\"", () -> {
            try (final TestCaseContainer container = createTestContainer(imageName)) {
                container.start();
                try {
                    LOG.info("Running tests test case container: {}", imageName);
                    final Container.ExecResult result = container.execInContainer("sh", "/app/run.sh");
                    LOG.info("STDOUT:\n{}", result.getStdout());
                    LOG.info("STDERR:\n{}", result.getStderr());
                    if (result.getExitCode() != 0) {
                        fail("Test case terminated with exit code " + result.getExitCode());
                    }
                } finally {
                    LOG.info("Terminating test case container: {}", imageName);
                    container.execInContainer("killall", "sleep");
                }
            }
        });
    }

    @NotNull
    private TestCaseContainer createTestContainer(final String imageName) {
        return new TestCaseContainer(
                imageName,
                TEST_TAG,
                "proxy:" + ProxyContainer.PROXY_BASE_PORT,
                kafkaContainer.getNetwork()
        );
    }
}
