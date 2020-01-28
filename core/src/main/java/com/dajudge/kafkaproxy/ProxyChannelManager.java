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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProxyChannelManager {
    private final Map<String, ProxyChannel> channels = new HashMap<>();
    private final ProxyChannelFactory channelFactory;

    public ProxyChannelManager(
            final ProxyChannelFactory channelFactory
    ) {
        this.channelFactory = channelFactory;
    }

    public synchronized Collection<ProxyChannel> proxies() {
        return channels.values();
    }

    public synchronized BrokerMapping getByBrokerEndpoint(final BrokerMapping.Endpoint brokerEndpoint) {
        final ProxyChannel channel = channels.computeIfAbsent(keyOf(brokerEndpoint), k ->
                channelFactory.create(this, brokerEndpoint)
        );
        channel.start();
        return new BrokerMapping(brokerEndpoint, new BrokerMapping.Endpoint(channel.getHost(), channel.getPort()));
    }

    private String keyOf(final BrokerMapping.Endpoint brokerEndpoint) {
        return brokerEndpoint.getHost() + ":" + brokerEndpoint.getPort();
    }

}
