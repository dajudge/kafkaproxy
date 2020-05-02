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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.protocol.KafkaMessageSplitter;
import com.dajudge.kafkaproxy.protocol.KafkaRequestProcessor;
import com.dajudge.kafkaproxy.protocol.KafkaRequestStore;
import com.dajudge.kafkaproxy.protocol.KafkaResponseProcessor;
import com.dajudge.kafkaproxy.protocol.rewrite.CompositeRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.FindCoordinatorRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.MetadataRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.ResponseRewriter;
import com.dajudge.proxybase.ProxyChannel;
import com.dajudge.proxybase.ProxyChannelFactory;
import com.dajudge.proxybase.ProxyContextFactory;
import com.dajudge.proxybase.Sink;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.buffer.ByteBuf;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class KafkaProxyChannelFactory {
    private final BrokerMapper brokerMapper;
    private final ProxyChannelFactory<ByteBuf, ByteBuf, ByteBuf, ByteBuf> proxyChannelFactory;

    public KafkaProxyChannelFactory(
            final BrokerMapper brokerMapper,
            final ProxyChannelFactory<ByteBuf, ByteBuf, ByteBuf, ByteBuf> proxyChannelFactory
    ) {
        this.brokerMapper = brokerMapper;
        this.proxyChannelFactory = proxyChannelFactory;
    }

    public ProxyChannel<ByteBuf, ByteBuf, ByteBuf, ByteBuf> create(
            final KafkaProxyChannelManager manager,
            final Endpoint endpoint
    ) {
        final BrokerMapping brokerToProxy = brokerMapper.getBrokerMapping(endpoint);
        if (brokerToProxy == null) {
            throw new IllegalArgumentException("No proxy configuration provided for " + endpoint);
        }
        final ProxyContextFactory<ByteBuf, ByteBuf, ByteBuf, ByteBuf> filterPairFactory = () -> {
            final ResponseRewriter rewriter = new CompositeRewriter(asList(
                    new MetadataRewriter(manager),
                    new FindCoordinatorRewriter(manager)
            ));
            final KafkaRequestStore requestStore = new KafkaRequestStore(rewriter);
            return new ProxyContextFactory.AbstractSimpleProxyContext() {
                @Override
                public Sink<ByteBuf> downstreamFilter(final Sink<ByteBuf> downstream) {
                    return new KafkaMessageSplitter(new KafkaRequestProcessor(downstream, requestStore));
                }

                @Override
                public Sink<ByteBuf> upstreamFilter(final Sink<ByteBuf> upstream) {
                    return new KafkaMessageSplitter(new KafkaResponseProcessor(upstream, requestStore));
                }
            };
        };
        return proxyChannelFactory.createProxyChannel(
                brokerToProxy.getProxy(),
                brokerToProxy.getBroker(),
                filterPairFactory
        );
    }

    public List<BrokerMapping> bootstrap(final KafkaProxyChannelManager manager) {
        return brokerMapper.getBootstrapBrokers().stream()
                .map(manager::getByBrokerEndpoint)
                .collect(toList());
    }
}
