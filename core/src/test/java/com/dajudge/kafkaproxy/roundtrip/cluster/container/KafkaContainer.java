/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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

package com.dajudge.kafkaproxy.roundtrip.cluster.container;

import com.dajudge.kafkaproxy.roundtrip.cluster.KafkaWaitStrategy;
import com.dajudge.kafkaproxy.roundtrip.comm.CommunicationSetup;
import com.dajudge.kafkaproxy.roundtrip.comm.ServerSecurity;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.dajudge.kafkaproxy.roundtrip.util.Util.indent;
import static java.lang.String.join;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class KafkaContainer extends GenericContainer<KafkaContainer> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaContainer.class);
    private static final int KAFKA_CLIENT_PORT = 9092;
    private static final int KAFKA_BROKER_PORT = 9093;
    private static final String ENTRYPOINT_PATH = "/customEntrypoint.sh";
    private final String internalHostname;
    private final ServerSecurity serverSecurity;

    public KafkaContainer(
            final ZookeeperContainer zookeeper,
            final int brokerId,
            final Network network,
            final CommunicationSetup communicationSetup
    ) {
        super("confluentinc/cp-kafka:5.4.1");
        this.internalHostname = "broker" + brokerId;
        serverSecurity = communicationSetup.getServerSecurity("CN=localhost");
        this.withNetwork(network)
                .withNetworkAliases(internalHostname)
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", zookeeper.getEndpoint())
                .withEnv("CONFLUENT_SUPPORT_METRICS_ENABLE", "0")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                .withEnv("KAFKA_BROKER_ID", valueOf(brokerId))
                .withEnv("KAFKA_NUM_PARTITIONS", "10")
                .withEnv("KAFKA_LISTENERS", join(",",
                        "CLIENT://:" + KAFKA_CLIENT_PORT,
                        "BROKER://:" + KAFKA_BROKER_PORT
                ))
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", join(",",
                        "CLIENT:" + serverSecurity.getClientProtocol(),
                        "BROKER:PLAINTEXT"
                ))
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                .withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", serverSecurity.getTrustStoreLocation())
                .withEnv("KAFKA_SSL_TRUSTSTORE_PASSWORD", serverSecurity.getTrustStorePassword())
                .withEnv("KAFKA_SSL_TRUSTSTORE_TYPE", serverSecurity.getTrustStoreType())
                .withEnv("KAFKA_SSL_KEYSTORE_LOCATION", serverSecurity.getKeyStoreLocation())
                .withEnv("KAFKA_SSL_KEYSTORE_PASSWORD", serverSecurity.getKeyStorePassword())
                .withEnv("KAFKA_SSL_KEYSTORE_TYPE", serverSecurity.getKeyStoreType())
                .withEnv("KAFKA_SSL_KEY_PASSWORD", serverSecurity.getKeyPassword())
                .withEnv("KAFKA_SSL_CLIENT_AUTH", serverSecurity.getClientAuth())
                .withExposedPorts(KAFKA_CLIENT_PORT)
                .withCopyFileToContainer(entrypointScript(), ENTRYPOINT_PATH)
                .withCommand("sh", ENTRYPOINT_PATH)
                .waitingFor(new KafkaWaitStrategy(
                        KAFKA_CLIENT_PORT,
                        communicationSetup.getClientSecurity()
                ))
                .withStartupTimeout(Duration.ofSeconds(300))
                .withLogConsumer(new Slf4jLogConsumer(LOG));
    }

    @NotNull
    private MountableFile entrypointScript() {
        return forClasspathResource("customEntrypoint.sh", 777);
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        final String configFile = configFile();
        LOG.info("Uploading keystores to container...");
        serverSecurity.uploadKeyStores(this::copyFileToContainer);
        LOG.info("Writing config to container:\n{}", indent(4, configFile));
        copyFileToContainer(Transferable.of(configFile.getBytes(UTF_8)), "/tmp/config.env");
    }

    private String configFile() {
        final Map<String, String> config = new HashMap<String, String>() {{
            put("KAFKA_ADVERTISED_LISTENERS", join(",",
                    "CLIENT://localhost:" + getMappedPort(KAFKA_CLIENT_PORT),
                    "BROKER://" + internalHostname + ":" + KAFKA_BROKER_PORT
            ));
        }};
        return config.entrySet().stream()
                .map((e) -> e.getKey() + "=" + e.getValue())
                .collect(joining("\n"));
    }

    public String getClientEndpoint() {
        return "localhost:" + getMappedPort(KAFKA_CLIENT_PORT);
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
