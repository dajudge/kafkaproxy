package com.dajudge.kafkaproxy.util.kafka;

import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestKeystore;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.reverse;
import static org.testcontainers.utility.MountableFile.forHostPath;

public class KafkaClusterWithSslBuilder extends KafkaClusterBuilder<KafkaClusterWithSslBuilder, KafkaClusterWihtSsl> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaClusterWithSslBuilder.class);
    private static final String KAFKA_SECRETS_DIR = "/etc/kafka/secrets/";
    private static final String KEYSTORE_PASSWORD_FILENAME = "keystore.pwd";
    private static final String KEY_PASSWORD_FILENAME = "key.pwd";
    private static final String KEYSTORE_FILENAME = "keystore.jks";
    private static final String TRUSTSTORE_LOCATION = KAFKA_SECRETS_DIR + "truststore.jks";
    private static final String KEYSTORE_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_FILENAME;
    private static final String KEYSTORE_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEYSTORE_PASSWORD_FILENAME;
    private static final String KEY_PASSWORD_LOCATION = KAFKA_SECRETS_DIR + KEY_PASSWORD_FILENAME;

    private final SslTestSetup.Builder sslSetupBuilder;

    private KafkaClusterWithSslBuilder(final SslTestSetup.Builder sslSetupBuilder) {
        this.sslSetupBuilder = sslSetupBuilder;
    }

    public KafkaClusterWihtSsl build() {
        LOG.info("Building cluster with brokers {}...", brokers);
        final SslTestSetup sslSetup = sslSetupBuilder.withBrokers(brokers).build();
        final List<AutoCloseable> resources = new ArrayList<>();
        final int zkPort = 2181;
        final Network network = Network.newNetwork();
        resources.add(network);
        final GenericContainer zk = zk(network, zkPort);
        resources.add(zk);
        zk.start();
        final AtomicInteger brokerId = new AtomicInteger();
        final Map<String, Integer> portMap = new HashMap<>();
        brokers.forEach(broker -> {
            final GenericContainer kafka = kafkaSsl(
                    network,
                    broker,
                    brokerId.incrementAndGet(),
                    sslSetup.getAuthority(),
                    sslSetup.getBroker(broker),
                    zkPort
            );
            kafka.start();
            resources.add(kafka);
            portMap.put(broker, kafka.getMappedPort(9092));
        });
        reverse(resources); // Ensure proper close order
        return new KafkaClusterWihtSsl(resources, portMap, sslSetup);
    }

    public static KafkaClusterWithSslBuilder kafkaClusterWithSsl(final SslTestSetup.Builder sslSetupBuilder) {
        return new KafkaClusterWithSslBuilder(sslSetupBuilder);
    }

    private GenericContainer kafkaSsl(
            final Network network,
            final String hostname,
            final int brokerId,
            final SslTestAuthority ca,
            final SslTestKeystore keystore,
            final int zkPort
    ) {
        return withSsl(hostname, kafkaContainer(hostname, brokerId, network, zkPort), ca, keystore);
    }

    private static GenericContainer withSsl(
            final String hostname,
            final GenericContainer kafkaContainer,
            final SslTestAuthority ca,
            final SslTestKeystore keystore
    ) {
        return kafkaContainer.withCopyFileToContainer(
                forHostPath(ca.getTrustStore().getAbsolutePath()),
                TRUSTSTORE_LOCATION
        )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeyStore().getAbsolutePath()),
                        KEYSTORE_LOCATION
                )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeystorePasswordFile().getAbsolutePath()),
                        KEYSTORE_PASSWORD_LOCATION
                )
                .withCopyFileToContainer(
                        forHostPath(keystore.getKeyPasswordFile().getAbsolutePath()),
                        KEY_PASSWORD_LOCATION
                )
                .withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", TRUSTSTORE_LOCATION)
                .withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", ca.getTrustStorePassword())
                .withEnv("KAFKA_SSL_KEYSTORE_FILENAME", KEYSTORE_FILENAME)
                .withEnv("KAFKA_SSL_KEYSTORE_CREDENTIALS", KEYSTORE_PASSWORD_FILENAME)
                .withEnv("KAFKA_SSL_KEY_CREDENTIALS", KEY_PASSWORD_FILENAME)
                .withEnv("KAFKA_LISTENERS", "SSL://0.0.0.0:9092,PLAINTEXT://localhost:9093")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT");
    }

    @Override
    protected String advertisedListeners(final String hostname, final int port) {
        return "SSL://" + hostname + ":" + port + ",PLAINTEXT://localhost:9093";
    }
}
