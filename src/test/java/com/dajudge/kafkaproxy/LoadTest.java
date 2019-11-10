package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMapper;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.load.ProducerLoop;
import com.dajudge.kafkaproxy.networking.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.ProxyChannel;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class LoadTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoadTest.class);

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

    @ClassRule
    public static final TemporaryFolder SSL_TEMP_DIR = createTempFolder();
    private static final String SSL_CA_DN = "CN=LoadTestCA";
    private static final SslTestSetup SSL_TEST_SETUP = SslTestSetup.builder(SSL_CA_DN, SSL_TEMP_DIR.getRoot())
            .withBrokers(range(0, BROKERS).mapToObj(i -> "kafka" + (i + 1)).collect(toList()))
            .build();
    private static final DockerMachine DOCKER_MACHINE = addEnvVars(DockerMachine.localMachine()).build();
    @ClassRule
    public static final DockerComposeRule DOCKER_RULE = waitForKafka(DockerComposeRule.builder()
            .file("src/test/resources/docker-compose.yml")
            .machine(DOCKER_MACHINE)
            .saveLogsTo("."))
            .build();

    private final Collection<NioEventLoopGroup> eventLoopGroups = new ArrayList<>();
    private final Collection<ProxyChannel> proxyChannels = new ArrayList<>();

    @Before
    public void setup() {
        for (int i = 0; i < BROKERS; i++) {
            final KafkaSslConfig kafkaSslConfig = new KafkaSslConfig(
                    true,
                    SSL_TEST_SETUP.getAuthority().getTrustStore(),
                    SSL_TEST_SETUP.getAuthority().getPassword(),
                    false
            );
            proxyChannels.add(new ProxyChannel(
                    PROXY_CHANNEL_PORT + i,
                    "localhost",
                    KAFKA_PORT + i,
                    kafkaSslConfig,
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
    public void works_for_a_long_time() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + PROXY_CHANNEL_PORT);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000);
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            final ProducerLoop producerLoop = new ProducerLoop(producer);
            final Thread producerThread = new Thread(producerLoop::run);
            producerThread.start();
            try {
                final long start = currentTimeMillis();
                long lastMessage = start;
                while ((currentTimeMillis() - start) < 1000 * 60 * 10) {
                    if ((currentTimeMillis() - lastMessage) > 1000) {
                        lastMessage = currentTimeMillis();
                        LOG.info("Produced {} messages", producerLoop.getProducedMessageCount());
                    }
                    if (System.in.available() > 0) {
                        break;
                    }
                    Thread.yield();
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
                producerThread.interrupt();
            }
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

    private static DockerMachine.LocalBuilder addEnvVars(DockerMachine.LocalBuilder localMachine) {
        for (int i = 0; i < BROKERS; i++) {
            final int brokerId = i + 1;
            final SslTestKeystore brokerKeyStore = SSL_TEST_SETUP.getBroker("kafka" + brokerId);
            final String prefix = "TEST_BROKER" + brokerId + "_";
            final String path = brokerKeyStore.getPath();
            final String keystorePassword = brokerKeyStore.getKeystorePassword().getAbsolutePath();
            final String keyPassword = brokerKeyStore.getKeyPassword().getAbsolutePath();
            localMachine = withEnv(localMachine, prefix + "KEYSTORE", path);
            localMachine = withEnv(localMachine, prefix + "KEYSTORE_PASSWORD", keystorePassword);
            localMachine = withEnv(localMachine, prefix + "KEY_PASSWORD", keyPassword);

        }
        final String trustStorePath = SSL_TEST_SETUP.getAuthority().getTrustStore().getAbsolutePath();
        final String trustStorePassword = SSL_TEST_SETUP.getAuthority().getPassword();
        localMachine = withEnv(localMachine, "TEST_TRUSTSTORE", trustStorePath);
        localMachine = withEnv(localMachine, "TEST_TRUSTSTORE_PASSWORD", trustStorePassword);
        return localMachine;
    }

    private static DockerMachine.LocalBuilder withEnv(
            final DockerMachine.LocalBuilder localMachine,
            final String key,
            final String value
    ) {
        LOG.debug("Set ENV for docker compose: {}={}", key, value);
        return localMachine.withAdditionalEnvironmentVariable(key, value);
    }

    private static TemporaryFolder createTempFolder() {
        final TemporaryFolder temporaryFolder = new TemporaryFolder();
        try {
            temporaryFolder.create();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return temporaryFolder;
    }
}
