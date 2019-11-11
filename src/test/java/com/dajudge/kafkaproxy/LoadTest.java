package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.load.ProducerLoop;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerMachine;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jetbrains.annotations.NotNull;
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
import java.util.concurrent.ExecutionException;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class LoadTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoadTest.class);

    private static final int PROXY_CHANNEL_PORT = 19091;
    private static final int KAFKA_PORT = 9091;
    private static final int BROKERS = 3;
    private static final BrokerMap BROKER_MAPPER = new BrokerMap(new HashMap<String, BrokerMapping>() {{
        for (int i = 0; i < BROKERS; i++) {
            final int kafkaPort = KAFKA_PORT + i;
            final int proxyPort = PROXY_CHANNEL_PORT + i;
            final int brokerId = i + 1;
            final BrokerMapping mapping = new BrokerMapping(
                    "broker" + (i + 1),
                    "localhost",
                    proxyPort,
                    "localhost",
                    proxyPort
            );
            put("kafka" + brokerId + ":" + kafkaPort, mapping);
        }
    }});

    @ClassRule
    public static final TemporaryFolder KAFKA_SSL_TEMP_DIR = createTempFolder();
    @ClassRule
    public static final TemporaryFolder CLIENT_SSL_TEMP_DIR = createTempFolder();
    private static final String KAFKA_SSL_CA_DN = "CN=KafkaCA";
    private static final String CLIENT_SSL_CA_DN = "CN=ClientCA";
    private static final SslTestSetup KAFKA_SSL_TEST_SETUP = SslTestSetup.builder(KAFKA_SSL_CA_DN, KAFKA_SSL_TEMP_DIR.getRoot())
            .withBrokers(range(0, BROKERS).mapToObj(i -> "kafka" + (i + 1)).collect(toList()))
            .build();
    private static final SslTestSetup CLIENT_SSL_TEST_SETUP = SslTestSetup.builder(CLIENT_SSL_CA_DN, CLIENT_SSL_TEMP_DIR.getRoot())
            .withBrokers(singletonList("localhost"))
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
        final SslTestKeystore proxyKeystore = CLIENT_SSL_TEST_SETUP.getBroker("localhost");
        for (int i = 0; i < BROKERS; i++) {
            final KafkaSslConfig kafkaSslConfig = new KafkaSslConfig(
                    true,
                    KAFKA_SSL_TEST_SETUP.getAuthority().getTrustStore(),
                    KAFKA_SSL_TEST_SETUP.getAuthority().getTrustStorePassword(),
                    false
            );
            final ProxySslConfig proxySslConfig = new ProxySslConfig(
                    true,
                    CLIENT_SSL_TEST_SETUP.getAuthority().getTrustStore(),
                    CLIENT_SSL_TEST_SETUP.getAuthority().getTrustStorePassword(),
                    proxyKeystore.getKeyStore(),
                    proxyKeystore.getKeystorePassword(),
                    proxyKeystore.getKeyPassword()
            );
            proxyChannels.add(new ProxyChannel(
                    PROXY_CHANNEL_PORT + i,
                    "localhost",
                    KAFKA_PORT + i,
                    proxySslConfig,
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
    public void sends_once() throws ExecutionException, InterruptedException {
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig(), new StringSerializer(), new StringSerializer())) {
            final ProducerRecord<String, String> record = new ProducerRecord<>("test.topic", "test", "test");
            producer.send(record).get();
        }
    }


    @Test
    public void works_for_a_long_time() {
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig(), new StringSerializer(), new StringSerializer())) {
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

    @NotNull
    private Map<String, Object> producerConfig() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + PROXY_CHANNEL_PORT);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, CLIENT_SSL_TEST_SETUP.getAuthority().getTrustStore().getAbsolutePath());
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, CLIENT_SSL_TEST_SETUP.getAuthority().getTrustStorePassword());
        return props;
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
            final SslTestKeystore brokerKeyStore = KAFKA_SSL_TEST_SETUP.getBroker("kafka" + brokerId);
            final String prefix = "TEST_BROKER" + brokerId + "_";
            final String path = brokerKeyStore.getKeyStore().getAbsolutePath();
            final String keystorePassword = brokerKeyStore.getKeystorePasswordFile().getAbsolutePath();
            final String keyPassword = brokerKeyStore.getKeyPasswordFile().getAbsolutePath();
            localMachine = withEnv(localMachine, prefix + "KEYSTORE", path);
            localMachine = withEnv(localMachine, prefix + "KEYSTORE_PASSWORD", keystorePassword);
            localMachine = withEnv(localMachine, prefix + "KEY_PASSWORD", keyPassword);

        }
        final String trustStorePath = KAFKA_SSL_TEST_SETUP.getAuthority().getTrustStore().getAbsolutePath();
        final String trustStorePassword = KAFKA_SSL_TEST_SETUP.getAuthority().getTrustStorePassword();
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
