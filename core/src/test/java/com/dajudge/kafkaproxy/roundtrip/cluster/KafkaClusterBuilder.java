/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import com.dajudge.kafkaproxy.KafkaProxyApplication;
import com.dajudge.kafkaproxy.roundtrip.client.ClientFactory;
import com.dajudge.kafkaproxy.roundtrip.cluster.container.KafkaContainer;
import com.dajudge.kafkaproxy.roundtrip.cluster.container.ZookeeperContainer;
import com.dajudge.kafkaproxy.roundtrip.comm.ClientSecurity;
import com.dajudge.kafkaproxy.roundtrip.comm.ClientSslConfig;
import com.dajudge.kafkaproxy.roundtrip.comm.CommunicationSetup;
import com.dajudge.kafkaproxy.roundtrip.comm.ServerSecurity;
import com.dajudge.kafkaproxy.roundtrip.util.PortFinder;
import com.dajudge.kafkaproxy.roundtrip.util.TestEnvironment;
import com.dajudge.kafkaproxy.roundtrip.util.TestFilesystem;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.dajudge.kafkaproxy.roundtrip.util.Util.indent;
import static com.dajudge.kafkaproxy.roundtrip.util.Util.safeToString;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.testcontainers.containers.Network.newNetwork;

public class KafkaClusterBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaClusterBuilder.class);
    private CommunicationSetup brokerComm;
    private CommunicationSetup proxyComm;

    public KafkaClusterBuilder withKafka(final CommunicationSetup communicationSetup) {
        brokerComm = communicationSetup;
        return this;
    }

    public KafkaClusterBuilder withProxy(final CommunicationSetup communicationSetup) {
        proxyComm = communicationSetup;
        return this;
    }

    public TestSetup build() {
        final KafkaCluster kafka = buildKafkaCluster();
        final int proxyBootstrapPort = freePort();
        @SuppressWarnings("PMD.CloseResource")
        final KafkaProxyApplication proxy = buildProxyApp(kafka, proxyBootstrapPort);
        final ClientFactory proxiedClientFactory = buildProxiedClientFactory(proxyBootstrapPort);
        final ClientFactory directClientFactory = buildDirectClientFactory(kafka);
        return new TestSetup(kafka, proxy, proxiedClientFactory, directClientFactory, proxyBootstrapPort);
    }

    private int freePort() {
        try (final PortFinder portFinder = new PortFinder()) {
            return portFinder.nextPort();
        }
    }

    private ClientFactory buildProxiedClientFactory(final int bootstrapPort) {
        return new ClientFactory("localhost:" + bootstrapPort, proxyComm.getClientSecurity());
    }

    private ClientFactory buildDirectClientFactory(final KafkaCluster kafka) {
        return new ClientFactory(kafka.getBootstrapServers(), brokerComm.getClientSecurity());
    }

    @NotNull
    private KafkaProxyApplication buildProxyApp(
            final KafkaCluster kafka,
            final int bootstrapPort
    ) {
        final ClientSecurity brokerSecurity = brokerComm.getClientSecurity();
        final ServerSecurity proxySecurity = proxyComm.getServerSecurity("CN=localhost");
        final ClientSslConfig proxyClient = brokerSecurity.newClient("CN=proxy");
        final TestEnvironment env = new TestEnvironment()
                .withEnv("KAFKAPROXY_BOOTSTRAP_SERVERS", kafka.getBootstrapServerList().iterator().next())
                .withEnv("KAFKAPROXY_BASE_PORT", valueOf(bootstrapPort))
                .withEnv("KAFKAPROXY_HOSTNAME", "localhost")
                .withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", valueOf("SSL".equals(brokerSecurity.getProtocol())))
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", brokerSecurity.getTrustStoreLocation())
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", safeToString(brokerSecurity.getTrustStorePassword()))
                .withEnv("KAFKAPROXY_KAFKA_SSL_KEYSTORE_LOCATION", proxyClient.getKeyStoreLocation())
                .withEnv("KAFKAPROXY_KAFKA_SSL_KEYSTORE_PASSWORD", safeToString(proxyClient.getKeyStorePassword()))
                .withEnv("KAFKAPROXY_KAFKA_SSL_KEYSTORE_TYPE", proxyClient.getKeyStoreType())
                .withEnv("KAFKAPROXY_KAFKA_SSL_KEY_PASSWORD", safeToString(proxyClient.getKeyPassword()))
                .withEnv("KAFKAPROXY_KAFKA_SSL_CLIENT_CERT_STRATEGY", proxyClient.getProxyCertStrategy())
                .withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", valueOf("SSL".equals(proxySecurity.getClientProtocol())))
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", proxySecurity.getTrustStoreLocation())
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", safeToString(proxySecurity.getTrustStorePassword()))
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_TYPE", proxySecurity.getTrustStoreType())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", proxySecurity.getKeyStoreLocation())
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", safeToString(proxySecurity.getKeyPassword()));
        final TestFilesystem filesystem = new TestFilesystem()
                .withFile(proxySecurity.getTrustStoreLocation(), proxySecurity.getTrustStore())
                .withFile(proxySecurity.getKeyStoreLocation(), proxySecurity.getKeyStore())
                .withFile(brokerSecurity.getTrustStoreLocation(), read(brokerSecurity.getTrustStoreLocation()))
                .withFile(proxyClient.getKeyStoreLocation(), read(proxyClient.getKeyStoreLocation()));
        passwordFile(env, filesystem, "KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD_LOCATION", proxySecurity.getKeyStorePassword());
        final StringBuilder buffer = new StringBuilder();
        env.dump(line -> buffer.append(line + "\n"));
        LOG.info("ENV:\n{}", indent(4, buffer.toString()));
        return KafkaProxyApplication.create(env, System::currentTimeMillis, filesystem);
    }

    private void passwordFile(
            final TestEnvironment env,
            final TestFilesystem filesystem,
            final String varName,
            final char[] password
    ) {
        if (password != null) {
            final String path = UUID.randomUUID().toString();
            env.withEnv(varName, path);
            filesystem.withFile(path, safeToString(password).getBytes(UTF_8));
        }
    }

    private byte[] read(final String path) {
        if (path == null) {
            return null;
        }
        try {
            return Files.readAllBytes(new File(path).toPath());
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    private KafkaCluster buildKafkaCluster() {
        @SuppressWarnings("PMD.CloseResource")
        final Network network = newNetwork();
        @SuppressWarnings("PMD.CloseResource")
        final ZookeeperContainer zookeeper = new ZookeeperContainer(network);
        zookeeper.start();
        final List<KafkaContainer> kafkaContainers = rangeClosed(1, 3)
                .mapToObj(i -> new KafkaContainer(zookeeper, i, network, brokerComm))
                .map(StarterThread::new)
                .parallel()
                .peek(Thread::start)
                .peek(StarterThread::waitForStartup)
                .map(StarterThread::getContainer)
                .collect(toList());
        final String bootstrapServers = kafkaContainers.stream()
                .map(KafkaContainer::getClientEndpoint)
                .collect(joining(","));
        return new KafkaCluster(join(singletonList(zookeeper), kafkaContainers), bootstrapServers);
    }

    @SafeVarargs
    @NotNull
    private final <T> List<T> join(final List<? extends T>... collections) {
        return stream(collections).flatMap(Collection::stream).collect(toList());
    }

}
