package com.dajudge.kafkaproxy.config.broker;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;

import java.util.Collection;

public class BrokerConfig {
    private final BrokerMap brokerMap;
    private final Collection<BrokerMapping> brokersToProxy;

    public BrokerConfig(final BrokerMap brokerMap, final Collection<BrokerMapping> brokersToProxy) {
        this.brokerMap = brokerMap;
        this.brokersToProxy = brokersToProxy;
    }

    public BrokerMap getBrokerMap() {
        return brokerMap;
    }

    public Collection<BrokerMapping> getBrokersToProxy() {
        return brokersToProxy;
    }
}
