package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMapper;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.networking.ProxyChannel;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class LoadTest {
    private static final int PROXY_CHANNEL_PORT = 19091;
    private static final int KAFKA_PORT = 9091;
    private static final int BROKERS = 3;
    private static final BrokerMapper BROKER_MAPPER = new BrokerMapper(new HashMap<String, BrokerMapping>() {{
        for (int i = 0; i < BROKERS; i++) {
            final int kafkaPort = KAFKA_PORT + i;
            final int proxyPort = PROXY_CHANNEL_PORT + i;
            final int brokerId = i + 1;
            put("kafka" + brokerId + ":" + kafkaPort, new BrokerMapping("localhost", proxyPort));
        }
    }});

    private final Collection<NioEventLoopGroup> eventLoopGroups = new ArrayList<>();
    private final Collection<ProxyChannel> proxyChannels = new ArrayList<>();

    @ClassRule
    public static DockerComposeRule docker = waitForKafka(DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .saveLogsTo("."))
            .build();

    @Before
    public void setup() {
        for (int i = 0; i < BROKERS; i++) {
            proxyChannels.add(new ProxyChannel(
                    PROXY_CHANNEL_PORT + i,
                    "localhost",
                    KAFKA_PORT + i,
                    BROKER_MAPPER,
                    newGroup(),
                    newGroup(),
                    newGroup()
            ));
        }
    }

    @After
    public void shutdown() {
        proxyChannels.forEach(ProxyChannel::close);
        eventLoopGroups.forEach(NioEventLoopGroup::shutdownGracefully);
    }

    @Test
    public void can_connect() {
        trySend("localhost:" + PROXY_CHANNEL_PORT);
    }

    private void trySend(final String bootstrapServers) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000);
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            final MementoCallback callback = new MementoCallback();
            try {
                producer.send(
                        new ProducerRecord<>("topic", "key", "value"),
                        callback
                ).get();
            } catch (final InterruptedException | ExecutionException e) {
                throw new AssertionError(e);
            }
            assertTrue(MyTest.waitFor(callback::isCompleted, 5000));
        }
    }

    private NioEventLoopGroup newGroup() {
        final NioEventLoopGroup group = new NioEventLoopGroup();
        eventLoopGroups.add(group);
        return group;
    }

    private static DockerComposeRule.Builder waitForKafka(DockerComposeRule.Builder builder) {
        for (int i = 0; i < BROKERS; i++) {
            builder = builder.waitingForService("kafka" + (i + 1), Container::areAllPortsOpen);
        }
        return builder;
    }
}
