package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.broker.BrokerMapParser;
import com.dajudge.kafkaproxy.roundtrip.RoundtripTester;
import com.dajudge.kafkaproxy.roundtrip.SingleRoundtrip;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import com.dajudge.kafkaproxy.util.kafka.KafkaClusterWihtSsl;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.dajudge.kafkaproxy.util.brokermap.BrokerMapBuilder.brokerMapFile;
import static com.dajudge.kafkaproxy.util.kafka.KafkaClusterWithSslBuilder.kafkaClusterWithSsl;
import static com.dajudge.kafkaproxy.util.ssl.SslTestSetup.sslSetup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

public class RoundtripTest {
    @ClassRule
    public static final TemporaryFolder KAFKA_SSL_TEMP_DIR = createTempFolder();
    public static final TemporaryFolder CLIENT_SSL_TEMP_DIR = createTempFolder();

    private final SslTestSetup clientSslSetup = sslSetup("CN=ClientCA", CLIENT_SSL_TEMP_DIR.getRoot())
            .withBrokers(singletonList("localhost"))
            .build();


    private Startup proxyApp;
    private String bootstrapServers;
    private KafkaClusterWihtSsl kafkaCluster;

    @Before
    public void setup() {
        kafkaCluster = kafkaClusterWithSsl(sslSetup("CN=KafkaCA", KAFKA_SSL_TEMP_DIR.getRoot()))
                .withBroker("localhost")
                .build();

        final byte[] brokerMapFile = brokerMapFile(kafkaCluster);
        final BrokerMap brokermap = new BrokerMapParser(new ByteArrayInputStream(brokerMapFile)).getBrokerMap();
        System.out.println(brokermap);
        final BrokerMapping firstBroker = brokermap.getAll().get(0);
        bootstrapServers = firstBroker.getProxy().getHost() + ":" + firstBroker.getProxy().getPort();

        final Environment env = new TestEnvironment()
                .withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", "client/truststore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", clientSslSetup.getAuthority().getTrustStorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", "client/keystore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", clientSslSetup.getBroker("localhost").getKeystorePassword())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", clientSslSetup.getBroker("localhost").getKeyPassword())
                .withFile("client/truststore.jks", clientSslSetup.getAuthority().getTrustStore())
                .withFile("client/keystore.jks", clientSslSetup.getBroker("localhost").getKeyStore())
                .withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", "kafka/truststore.jks")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", kafkaCluster.getAuthority().getTrustStorePassword())
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true")
                .withFile("kafka/truststore.jks", kafkaCluster.getAuthority().getTrustStore())
                .withFile("/etc/kafkaproxy/brokermap.yml", brokerMapFile);

        proxyApp = new Startup(env);
        proxyApp.start();
    }

    @After
    public void shutdown() {
        proxyApp.shutdown();
        kafkaCluster.close();
    }

    @Test
    public void test_roundtrip() {
        final Map<String, Object> baseProps = new HashMap<>();
        baseProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        baseProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        baseProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientSslSetup.getAuthority().getTrustStore().getAbsolutePath());
        baseProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, clientSslSetup.getAuthority().getTrustStorePassword());

        final Map<String, Object> producerProps = new HashMap<>(baseProps);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        final Map<String, Object> consumerProps = new HashMap<>(baseProps);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testgroup");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final RoundtripTester tester = new RoundtripTester(producerProps, consumerProps, 1);
        final SingleRoundtrip roundtrip = new SingleRoundtrip(5000);
        tester.run(roundtrip);
        assertTrue("Did not complete roundtrip", roundtrip.completed());
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
