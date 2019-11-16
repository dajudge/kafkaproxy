/*
 * Copyright 2019 Alex Stockinger
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

package com.dajudge.kafkaproxy.util.kafka;

import com.dajudge.kafkaproxy.util.ssl.SslTestAuthority;
import com.dajudge.kafkaproxy.util.ssl.SslTestSetup;

import java.util.List;
import java.util.Map;

public class KafkaClusterWihtSsl extends KafkaCluster {
    private final SslTestSetup sslSetup;

    /**
     * Constructor.
     *
     * @param resources the resources to close on shutdown. Guaranteed to be destroyed in order
     *                  of list.
     * @param brokers   map from broker name to port of broker.
     * @param sslSetup
     */
    KafkaClusterWihtSsl(
            final List<AutoCloseable> resources,
            final Map<String, Integer> brokers,
            final SslTestSetup sslSetup
    ) {
        super(resources, brokers);
        this.sslSetup = sslSetup;
    }

    public SslTestAuthority getAuthority() {
        return sslSetup.getAuthority();
    }
}
