package com.dajudge.kafkaproxy.tests;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedSet;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class MultiClientTest {
    private static final int RUNTIME_SECS = 10;
    private final String bootstrapServers;

    public MultiClientTest(final String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    interface Sink extends AutoCloseable {
        void publish(String uid);
    }

    interface Poll extends AutoCloseable {
        List<String> poll();

        void cancel();
    }

    public boolean run() {
        System.out.println(format("Running multi-client test for %ss", RUNTIME_SECS));
        final long start = currentTimeMillis();
        final Set<String> messages = synchronizedSet(new HashSet<>());
        final String topicName = randomUUID().toString();
        final String groupId = randomUUID().toString();
        createTopic(bootstrapServers, topicName);
        final Supplier<Sink> sinkFactory = () -> producer(bootstrapServers, topicName);
        final Supplier<Poll> pollFactory = () -> consumer(bootstrapServers, topicName, groupId);
        int producedMessages = 0;
        try (
                final SinkMaster sinkMaster = new SinkMaster(messages, 10, sinkFactory);
                final PollMaster pollMaster = new PollMaster(messages, 20, pollFactory);
        ) {
            System.out.println("Producing to " + topicName);
            long lastStats = currentTimeMillis();
            while ((currentTimeMillis() - start) < RUNTIME_SECS * 1000) {
                if ((currentTimeMillis() - lastStats) > 1000) {
                    lastStats = currentTimeMillis();
                    System.out.println("sent: " + producedMessages + " inflight: " + messages.size());
                }
                sinkMaster.randomSink().publish("" + producedMessages);
                producedMessages++;
                if (producedMessages % 100 == 0) {
                    Thread.yield();
                }
            }
            System.out.println("Producing phase complete, waiting for consumers to finish...");
            while (!messages.isEmpty() && (currentTimeMillis() - pollMaster.lastMessageTimestamp()) < 10000) {
                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
            System.out.println("Produced messages: " + producedMessages);
            System.out.println("Unseen messages: " + messages);
        }
        final long duration = currentTimeMillis() - start;
        System.out.println("Test completed in " + duration + "ms");
        return messages.size() == 0;
    }

    private void createTopic(final String bootstrapServers, final String topicName) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (final AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(singletonList(new NewTopic(topicName, 20, (short) 1)));
        }
    }

    private Poll consumer(final String bootstrapServers, final String topicName, final String groupId) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, randomUUID().toString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(singletonList(topicName));
        return new Poll() {
            @Override
            public List<String> poll() {
                return stream(consumer.poll(500).spliterator(), false)
                        .map(ConsumerRecord::value)
                        .collect(toList());
            }

            @Override
            public void close() {
                consumer.close();
            }

            @Override
            public void cancel() {
                consumer.wakeup();
            }
        };
    }

    private Sink producer(final String bootstrapServers, final String topicName) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, randomUUID().toString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        final KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        return new Sink() {
            @Override
            public void publish(final String uid) {
                producer.send(new ProducerRecord<>(topicName, uid, uid));
            }

            @Override
            public void close() {
                producer.close();
            }
        };
    }

    private static class PollThread extends Thread {
        private final Poll poll;
        private final Set<String> messages;
        private final Runnable onMessage;

        public PollThread(final Poll poll, final Set<String> messages, final Runnable onMessage) {
            this.poll = poll;
            this.messages = messages;
            this.onMessage = onMessage;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    try {
                        final List<String> polledMessages = poll.poll();
                        if (!polledMessages.isEmpty()) {
                            messages.removeAll(polledMessages);
                            onMessage.run();
                        }
                    } catch (final WakeupException e) {
                        return;
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    poll.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void close() throws Exception {
            poll.cancel();
        }
    }

    private static class PollMaster implements AutoCloseable {
        private final List<PollThread> pollThreads = new ArrayList<>();
        private long lastMessageTimestamp = currentTimeMillis();

        public PollMaster(final Set<String> messages, final int pollThreadCount, final Supplier<Poll> pollFactory) {
            for (int i = 0; i < pollThreadCount; i++) {
                pollThreads.add(
                        new PollThread(pollFactory.get(), messages, () -> lastMessageTimestamp = currentTimeMillis())
                );
            }
            pollThreads.forEach(Thread::start);
        }

        public long lastMessageTimestamp() {
            return lastMessageTimestamp;
        }

        @Override
        public void close() {
            pollThreads.forEach(t -> {
                try {
                    t.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static class SinkMaster implements AutoCloseable {
        private static final Random RAND = new Random();
        private final List<Sink> sinks = new ArrayList<>();

        public SinkMaster(final Set<String> messages, final int sinkCount, final Supplier<Sink> sinkFactory) {
            for (int i = 0; i < sinkCount; i++) {
                final Sink delegate = sinkFactory.get();
                sinks.add(new Sink() {
                    @Override
                    public void publish(final String uid) {
                        messages.add(uid);
                        delegate.publish(uid);
                    }

                    @Override
                    public void close() throws Exception {
                        delegate.close();
                    }
                });
            }
        }

        public Sink randomSink() {
            return sinks.get(RAND.nextInt(sinks.size()));
        }

        @Override
        public void close() {
            sinks.forEach(s -> {
                try {
                    s.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
