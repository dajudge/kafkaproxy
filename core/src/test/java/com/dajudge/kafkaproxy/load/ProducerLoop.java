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
