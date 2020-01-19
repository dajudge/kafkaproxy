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

package com.dajudge.kafkaproxy.brokermap;

public class BrokerMapping {
    private final Endpoint broker;
    private final Endpoint proxy;

    public BrokerMapping(final Endpoint broker, final Endpoint proxy) {
        this.broker = broker;
        this.proxy = proxy;
    }

    public BrokerMapping(
            final String brokerHost,
            final int brokerPort,
            final String proxyHost,
            final int proxyPort
    ) {
        this(new Endpoint(brokerHost, brokerPort), new Endpoint(proxyHost, proxyPort));
    }

    public Endpoint getBroker() {
        return broker;
    }

    public Endpoint getProxy() {
        return proxy;
    }

    public static class Endpoint {
        private final String host;
        private final int port;

        public Endpoint(final String host, final int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "Endpoint{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BrokerMapping{" +
                "broker=" + broker +
                ", proxy=" + proxy +
                '}';
    }
}
