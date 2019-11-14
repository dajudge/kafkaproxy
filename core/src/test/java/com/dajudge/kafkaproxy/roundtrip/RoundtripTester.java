package com.dajudge.kafkaproxy.roundtrip;

import com.dajudge.kafkaproxy.roundtrip.RoundtripRunner.RoundtripConsumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;

public class RoundtripTester {
    private static final Logger LOG = LoggerFactory.getLogger(RoundtripTester.class);
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
            final int maxMessages
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
        this.runner = new RoundtripRunner<>(1, producerFactory, 1, consumerFactory, sink);
    }

    public void run(final AbortCondition abortCondition) {
        while (!abortCondition.check(messagesSent, messagesCompleted, inflight.size(), messagesUnknown)) {
            if (maxMessages <= 0 || messagesSent < maxMessages) {
                final String uuid = UUID.randomUUID().toString();
                inflight.add(uuid);
                runner.produce(TEST_TOPIC, uuid, "" + currentTimeMillis());
                messagesSent++;
            }
            Thread.yield();
        }
        runner.shutdown();
    }
}
