package com.dajudge.kafkaproxy.load;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.UUID;

import static java.lang.Thread.interrupted;

public class ProducerLoop {
    private final KafkaProducer<String, String> producer;
    private int producedMessages;

    public ProducerLoop(final KafkaProducer<String, String> producer) {
        this.producer = producer;
    }

    public void run() {
        while (!interrupted()) {
            final String key = UUID.randomUUID().toString();
            final ProducerRecord<String, String> record = new ProducerRecord<>(
                    "test.topic",
                    key,
                    String.valueOf(System.currentTimeMillis())
            );
            producer.send(record);
            producedMessages++;
            Thread.yield();
        }
    }

    public int getProducedMessageCount() {
        return producedMessages;
    }
}
