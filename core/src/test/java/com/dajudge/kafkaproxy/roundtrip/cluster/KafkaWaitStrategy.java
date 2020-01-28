/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import com.dajudge.kafkaproxy.roundtrip.client.ClientFactory;
import com.dajudge.kafkaproxy.roundtrip.comm.ClientSecurity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class KafkaWaitStrategy extends AbstractWaitStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaWaitStrategy.class);
    private final int internalClientPort;
    private final ClientSecurity clientSecurity;

    public KafkaWaitStrategy(final int internalClientPort, final ClientSecurity clientSecurity) {
        this.internalClientPort = internalClientPort;
        this.clientSecurity = clientSecurity;
    }

    @Override
    @SuppressFBWarnings(
            value="RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification="False positive" // see https://sourceforge.net/p/findbugs/bugs/1169/
    )
    protected void waitUntilReady() {
        final String bootstrapServers = "localhost:" + waitStrategyTarget.getMappedPort(internalClientPort);
        try (final AdminClient admin = new ClientFactory(bootstrapServers, clientSecurity).admin("CN=admin")) {
            admin.describeCluster().nodes().get(startupTimeout.getSeconds(), SECONDS).size();
        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to wait for kafka broker", e);
        }
        LOG.info("{} is available.", waitStrategyTarget.getContainerInfo().getName());
    }
}
