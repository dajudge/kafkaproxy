package com.dajudge.kafkaproxy.roundtrip;

import com.dajudge.kafkaproxy.roundtrip.RoundtripRunner.RoundtripConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

class ConsumerThread<K, V> extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerThread.class);
    private final KafkaConsumer<K, V> consumer;
    private final RoundtripConsumer<K, V> sink;

    ConsumerThread(
            final KafkaConsumer<K, V> consumer,
            final RoundtripConsumer<K, V> sink
    ) {
        this.consumer = consumer;
        this.sink = sink;
    }

    public void run() {
        try {
            LOG.info("Consumer thread started.");
            while (true) {
                try {
                    final ConsumerRecords<K, V> records = consumer.poll(Duration.of(1, SECONDS));
                    if (records.count() > 0) {
                        LOG.trace("Received {} records.", records.count());
                    }
                    records.forEach(this::consume);
                    consumer.commitAsync();
                } catch (final WakeupException e) {
                    throw e;
                } catch (final Exception e) {
                    LOG.error("Error processing consumed message", e);
                }
            }
        } catch (final WakeupException e) {
            LOG.info("Consumer thread exited.");
            return;
        } finally {
            consumer.close();
        }
    }

    private void consume(final ConsumerRecord<K, V> record) {
        sink.onMessage(record.topic(), record.key(), record.value());
    }
}
