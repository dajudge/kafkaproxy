package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMapper;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.networking.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.ProxyChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class MyTest {
    private static final int PROXY_CHANNEL_PORT = 51577;

    @Rule
    public KafkaContainer kafka = new KafkaContainer();

    private ProxyChannel channel;

    @Before
    public void start() {
        final Matcher matcher = Pattern.compile("^[^:]+://([^:]+):([0-9]+)$").matcher(kafka.getBootstrapServers());
        assertTrue("No match: " + kafka.getBootstrapServers(), matcher.matches());
        final String hostname = matcher.group(1);
        final int port = Integer.parseInt(matcher.group(2));
        final BrokerMapper brokerMapper = new BrokerMapper(new HashMap<String, BrokerMapping>() {{
            put(hostname + ":" + port, new BrokerMapping("localhost", PROXY_CHANNEL_PORT));
        }});
        final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        channel = new ProxyChannel(
                PROXY_CHANNEL_PORT,
                hostname,
                port,
                new KafkaSslConfig(false, null, null, false),
                brokerMapper,
                bossGroup,
                upstreamWorkerGroup,
                downstreamWorkerGroup
        );
    }

    @After
    public void stop() throws InterruptedException {
        channel.close().sync();
    }

    @Test
    public void doit() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + PROXY_CHANNEL_PORT);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
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
            assertTrue(waitFor(callback::isCompleted, 5000));
        }
    }

    public static boolean waitFor(final Supplier<Boolean> check, final int msecs) {
        final long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < msecs) {
            if (check.get()) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

}
