package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.networking.downstream.DownstreamChannelFactory;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import com.dajudge.kafkaproxy.roundtrip.RoundtripTester;
import com.dajudge.kafkaproxy.roundtrip.SingleRoundtrip;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class MyTest {
    private static final int PROXY_CHANNEL_PORT = 51577;

    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
    private final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();

    @Rule
    public KafkaContainer kafka = new KafkaContainer();

    private ProxyChannel channel;

    @Before
    public void start() {
        final Matcher matcher = Pattern.compile("^[^:]+://([^:]+):([0-9]+)$").matcher(kafka.getBootstrapServers());
        assertTrue("No match: " + kafka.getBootstrapServers(), matcher.matches());
        final String hostname = matcher.group(1);
        final int port = Integer.parseInt(matcher.group(2));
        final BrokerMap brokerMap = new BrokerMap(new ArrayList<BrokerMapping>() {{
            add(new BrokerMapping("broker", "localhost", port, hostname, PROXY_CHANNEL_PORT));
        }});
        final ForwardChannelFactory forwardChannelFactory = new DownstreamChannelFactory(
                brokerMap,
                hostname,
                port,
                new KafkaSslConfig(false, null, null, false),
                downstreamWorkerGroup
        );
        channel = new ProxyChannel(
                PROXY_CHANNEL_PORT,
                new ProxySslConfig(false, null, null, null, null, null),
                bossGroup,
                upstreamWorkerGroup,
                forwardChannelFactory
        );
    }

    @After
    public void stop() throws InterruptedException {
        channel.close().sync();
        bossGroup.shutdownGracefully();
        downstreamWorkerGroup.shutdownGracefully();
        upstreamWorkerGroup.shutdownGracefully();
    }

    @Test
    public void test_roundtrip() {
        final Map<String, Object> baseProps = new HashMap<>();
        baseProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + channel.getPort());

        final Map<String, Object> producerProps = new HashMap<>(baseProps);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000);
        final Map<String, Object> consumerProps = new HashMap<>(baseProps);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testgroup");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final RoundtripTester tester = new RoundtripTester(producerProps, consumerProps, 1);
        final SingleRoundtrip roundtrip = new SingleRoundtrip(60000);
        tester.run(roundtrip);
        assertTrue("Did not complete roundtrip", roundtrip.completed());
    }

}
