/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.kafkaproxy.config.broker;

import com.dajudge.kafkaproxy.brokermap.BrokerMapping.Endpoint;

import java.util.List;

public class BrokerConfig {
    private final List<Endpoint> bootstrapBrokers;
    private final String proxyHostname;
    private final int proxyBasePort;

    public BrokerConfig(final List<Endpoint> bootstrapBrokers, final String proxyHostname, final int proxyBasePort) {
        this.bootstrapBrokers = bootstrapBrokers;
        this.proxyHostname = proxyHostname;
        this.proxyBasePort = proxyBasePort;
    }

    public List<Endpoint> getBootstrapBrokers() {
        return bootstrapBrokers;
    }

    public String getProxyHostname() {
        return proxyHostname;
    }

    public int getProxyBasePort() {
        return proxyBasePort;
    }
}
