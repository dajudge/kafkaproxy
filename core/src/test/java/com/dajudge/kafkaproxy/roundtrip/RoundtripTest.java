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

package com.dajudge.kafkaproxy.roundtrip;

import com.dajudge.kafkaproxy.roundtrip.client.ClientFactory;
import com.dajudge.kafkaproxy.roundtrip.cluster.CommunicationSetupBuilder;
import com.dajudge.kafkaproxy.roundtrip.cluster.KafkaClusterBuilder;
import com.dajudge.kafkaproxy.roundtrip.cluster.TestSetup;
import com.dajudge.kafkaproxy.roundtrip.comm.CommunicationSetup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.dajudge.kafkaproxy.roundtrip.util.Util.randomIdentifier;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class RoundtripTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoundtripTest.class);

    private static class TestConfiguration {
        private final CommunicationSetup clientComm;
        private final CommunicationSetup brokerComm;

        private TestConfiguration(final CommunicationSetup clientComm, final CommunicationSetup brokerComm) {
            this.clientComm = clientComm;
            this.brokerComm = brokerComm;
        }
    }

    @Parameterized.Parameters
    public static Collection<TestConfiguration> data() {
        return asList(
                new TestConfiguration(plaintext(), plaintext()),
                new TestConfiguration(plaintext(), tls()),
                new TestConfiguration(plaintext(), mtls()),
                new TestConfiguration(tls(), tls()),
                new TestConfiguration(tls(), mtls()),
                new TestConfiguration(tls(), plaintext()),
                new TestConfiguration(mtls(), plaintext()),
                new TestConfiguration(mtls(), tls()),
                new TestConfiguration(mtls(), mtls())
        );
    }

    @Parameterized.Parameter
    public TestConfiguration config;

    @Test
    public void roundtrip_test() throws Exception {
        try (final TestSetup testSetup = createTestSetup()) {
            runRoundtripTest(
                    testSetup.getDirectClientFactory(),
                    testSetup.getProxiedClientFactory()
            );
        }
    }

    private TestSetup createTestSetup() {
        LOG.info("Broker comm: {}", config.brokerComm);
        LOG.info("Client comm: {}", config.clientComm);
        return new KafkaClusterBuilder()
                .withKafka(config.brokerComm)
                .withProxy(config.clientComm)
                .build();
    }

    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "False positive" // see https://sourceforge.net/p/findbugs/bugs/1169/
    )
    private void runRoundtripTest(
            final ClientFactory producerClientFactory,
            final ClientFactory consumerClientFactory
    ) throws InterruptedException, ExecutionException, TimeoutException {
        final String topic = randomIdentifier();
        final String groupId = randomIdentifier();
        final Set<String> keysToSend = IntStream.range(0, 100).mapToObj(i -> "key" + i)
                .collect(toSet());
        LOG.info("Creating topic {}...", topic);
        try (final AdminClient admin = producerClientFactory.admin("CN=admin")) {
            admin.createTopics(singletonList(new NewTopic(topic, 10, (short) 1))).all()
                    .get(30, SECONDS);
        }
        LOG.info("Producing message to {}...", topic);
        try (final KafkaProducer<String, String> producer = producerClientFactory.producer("CN=producer")) {
            keysToSend.forEach(key -> {
                try {
                    producer.send(new ProducerRecord<>(topic, key, "value")).get(30, SECONDS);
                } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        LOG.info("Consuming roundtrip message from {}...", topic);
        try (final KafkaConsumer<String, String> consumer = consumerClientFactory.consumer("CN=consumer", groupId)) {
            consumer.subscribe(singletonList(topic));
            final long start = currentTimeMillis();
            final HashSet<String> consumedKeys = new HashSet<>();
            while ((currentTimeMillis() - start) < 10000) {
                final ConsumerRecords<String, String> records = consumer.poll(ofMillis(100));
                if (records.isEmpty()) {
                    continue;
                }
                stream(records.spliterator(), false).forEach(r -> consumedKeys.add(r.key()));
                if (consumedKeys.equals(keysToSend)) {
                    return;
                }
            }
            fail("Roundtrip did not complete in time");
        }
    }

    private static CommunicationSetup tls() {
        return new CommunicationSetupBuilder().withSsl().build();
    }

    private static CommunicationSetup mtls() {
        return new CommunicationSetupBuilder().withMutualTls().build();
    }

    private static CommunicationSetup plaintext() {
        return new CommunicationSetupBuilder().withPlaintext().build();
    }
}
