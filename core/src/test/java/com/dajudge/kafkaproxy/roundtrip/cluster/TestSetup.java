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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import com.dajudge.kafkaproxy.KafkaProxyApplication;
import com.dajudge.kafkaproxy.roundtrip.client.ClientFactory;

public class TestSetup implements AutoCloseable {
    private final KafkaCluster kafka;
    private final KafkaProxyApplication proxy;
    private final ClientFactory proxiedClientFactory;
    private final ClientFactory directClientFactory;
    private final int proxyBootstrapPort;

    TestSetup(
            final KafkaCluster kafka,
            final KafkaProxyApplication proxy,
            final ClientFactory proxiedClientFactory,
            final ClientFactory directClientFactory,
            final int proxyBootstrapPort
    ) {
        this.kafka = kafka;
        this.proxy = proxy;
        this.proxiedClientFactory = proxiedClientFactory;
        this.directClientFactory = directClientFactory;
        this.proxyBootstrapPort = proxyBootstrapPort;
    }

    public ClientFactory getProxiedClientFactory() {
        return proxiedClientFactory;
    }

    public ClientFactory getDirectClientFactory() {
        return directClientFactory;
    }

    public KafkaCluster getKafka() {
        return kafka;
    }

    public KafkaProxyApplication getProxy() {
        return proxy;
    }

    @Override
    public void close() {
        kafka.close();
        proxy.close();
    }

    public int getProxyBootstrapPort() {
        return proxyBootstrapPort;
    }
}
