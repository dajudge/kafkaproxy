package com.dajudge.kafkaproxy.brokermap;

public class BrokerMapping {
    private final String host;
    private final int port;

    public BrokerMapping(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
