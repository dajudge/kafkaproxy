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
