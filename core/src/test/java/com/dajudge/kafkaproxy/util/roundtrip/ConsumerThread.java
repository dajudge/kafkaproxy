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

package com.dajudge.kafkaproxy.util.roundtrip;

import com.dajudge.kafkaproxy.util.roundtrip.RoundtripRunner.RoundtripConsumer;
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
            LOG.debug("Consumer thread started.");
            while (true) {
                try {
                    final ConsumerRecords<K, V> records = consumer.poll(Duration.of(1, SECONDS));
                    if (records.count() > 0) {
                        LOG.trace("Received {} records.", records.count());
                    }
                    consume(records);
                    consumer.commitAsync();
                } catch (final WakeupException e) {
                    throw e;
                } catch (final Exception e) {
                    LOG.error("Error processing consumed message", e);
                }
            }
        } catch (final WakeupException e) {
            return;
        } finally {
            consumer.close();
            LOG.debug("Consumer thread exited.");
        }
    }

    private void consume(final ConsumerRecords<K, V> records) {
        synchronized (sink) {
            records.forEach(record -> sink.onMessage(record.topic(), record.key(), record.value()));
        }
    }

}
