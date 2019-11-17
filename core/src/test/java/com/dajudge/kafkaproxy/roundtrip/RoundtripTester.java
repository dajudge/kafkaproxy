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

package com.dajudge.kafkaproxy.roundtrip;

import com.dajudge.kafkaproxy.roundtrip.RoundtripRunner.RoundtripConsumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;

public class RoundtripTester {
    private static final String TEST_TOPIC = "test.topic";
    private final RoundtripRunner<String, String> runner;
    private final Set<String> inflight = Collections.synchronizedSet(new HashSet<>());
    private final int maxMessages;
    private int messagesSent = 0;
    private int messagesCompleted = 0;
    private int messagesUnknown = 0;

    public RoundtripTester(
            final Map<String, Object> producerProperties,
            final Map<String, Object> consumerProperties,
            final int maxMessages,
            final int producers,
            final int consumers
    ) {
        this.maxMessages = maxMessages;
        final Supplier<KafkaProducer<String, String>> producerFactory = () -> new KafkaProducer<>(
                producerProperties,
                new StringSerializer(),
                new StringSerializer()
        );
        final Supplier<KafkaConsumer<String, String>> consumerFactory = () -> {
            final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                    consumerProperties,
                    new StringDeserializer(),
                    new StringDeserializer()
            );
            consumer.subscribe(singletonList(TEST_TOPIC));
            return consumer;
        };
        final RoundtripConsumer<String, String> sink = (topic, key, value) -> {
            if (inflight.remove(key)) {
                messagesCompleted++;
            } else {
                messagesUnknown++;
            }
        };
        this.runner = new RoundtripRunner<>(producers, producerFactory, consumers, consumerFactory, sink).start();
    }

    public void run(final AbortCondition abortCondition) {
        long loops = 0;
        while (!abortCondition.check(messagesSent, messagesCompleted, inflight.size(), messagesUnknown)) {
            if (maxMessages <= 0 || messagesSent < maxMessages) {
                final String uuid = "" + loops;
                inflight.add(uuid);
                runner.produce(TEST_TOPIC, uuid, "" + currentTimeMillis());
                messagesSent++;
            }
            if (++loops % 1000 == 0) {
                Thread.yield();
            }
        }
        runner.shutdown();
    }
}
