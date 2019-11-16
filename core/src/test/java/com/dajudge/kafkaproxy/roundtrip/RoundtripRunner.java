package com.dajudge.kafkaproxy.roundtrip;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class RoundtripRunner<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(RoundtripRunner.class);
    private static final Random RANDOM = new Random();
    private final Collection<Thread> threads = new ArrayList<>();
    private final List<KafkaProducer<K, V>> producers = new ArrayList<>();
    private final List<KafkaConsumer<K, V>> consumers = new ArrayList<>();

    public RoundtripRunner(
            final int producers,
            final Supplier<KafkaProducer<K, V>> producerFactory,
            final int consumers,
            final Supplier<KafkaConsumer<K, V>> consumerFactory,
            final RoundtripConsumer<K, V> datasink
    ) {
        for (int i = 0; i < producers; i++) {
            this.producers.add(producerFactory.get());
        }
        for (int i = 0; i < consumers; i++) {
            final KafkaConsumer<K, V> consumer = consumerFactory.get();
            final ConsumerThread thread = new ConsumerThread<>(consumer, datasink);
            thread.start();
            threads.add(thread);
            this.consumers.add(consumer);
        }
    }

    void produce(final String topic, final K key, final V value) {
        LOG.trace("Producing message: {}/{}/{}", topic, key, value);
        producers.get(RANDOM.nextInt(producers.size())).send(new ProducerRecord<>(
                topic,
                key,
                value
        ));
    }

    void shutdown() {
        LOG.info("Closing producers...");
        producers.forEach(KafkaProducer::close);
        LOG.info("Waking up consumers...");
        consumers.forEach(KafkaConsumer::wakeup);
        LOG.info("Joining consumer threads...");
        threads.forEach(t -> {
            try {
                t.join(10000);
                if (t.isAlive()) {
                    LOG.error("Consumer thread did not terminate in time.");
                }
            } catch (final InterruptedException e) {
                LOG.error("Interrupted while joining consumer thread.", e);
            }
        });
    }

    interface RoundtripConsumer<K, V> {
        void onMessage(String topic, K key, V value);
    }
}
