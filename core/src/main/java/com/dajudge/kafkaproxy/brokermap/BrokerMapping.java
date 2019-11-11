package com.dajudge.kafkaproxy.brokermap;

public class BrokerMapping {
    private final String name;
    private final Endpoint broker;
    private final Endpoint proxy;

    public BrokerMapping(final String name, final Endpoint broker, final Endpoint proxy) {
        this.name = name;
        this.broker = broker;
        this.proxy = proxy;
    }

    public BrokerMapping(
            final String name,
            final String brokerHost,
            final int brokerPort,
            final String proxyHost,
            final int proxyPort
    ) {
        this(name, new Endpoint(brokerHost, brokerPort), new Endpoint(proxyHost, proxyPort));
    }

    public String getName() {
        return name;
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
