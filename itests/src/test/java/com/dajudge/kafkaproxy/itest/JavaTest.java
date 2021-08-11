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

import com.dajudge.kafkaproxy.itest.util.ITest;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaTest extends BaseIntegrationTest {
    @ClassRule
    public static final ITest CONTAINER = new ITest("localhost/kafkaproxy/itest-java:latest")
            .withEnv("CONNECTION_ATTEMPTS", "5000");

    @Test
    public void run() {
        final int exitCode = withKafkaProxy(proxyEndpoint ->
                CONTAINER.exec("/usr/bin/java", "-jar", "/app/build/libs/test-java.jar", proxyEndpoint)
        );
        assertEquals("Test run failed", 0, exitCode);
    }
}
