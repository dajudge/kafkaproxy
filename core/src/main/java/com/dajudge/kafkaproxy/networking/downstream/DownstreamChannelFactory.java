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

package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannel;
import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory;
import com.dajudge.kafkaproxy.protocol.KafkaMessageSplitter;
import com.dajudge.kafkaproxy.protocol.KafkaRequestProcessor;
import com.dajudge.kafkaproxy.protocol.KafkaRequestStore;
import com.dajudge.kafkaproxy.protocol.KafkaResponseProcessor;
import com.dajudge.kafkaproxy.protocol.rewrite.CompositeRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.FindCoordinatorRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.MetadataRewriter;
import com.dajudge.kafkaproxy.protocol.rewrite.ResponseRewriter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

import java.util.function.Consumer;

import static java.util.Arrays.asList;

public class DownstreamChannelFactory implements ForwardChannelFactory {
    private final BrokerMap brokerMap;
    private final String kafkaHost;
    private final int kafkaPort;
    private final KafkaSslConfig kafkaSslConfig;
    private final EventLoopGroup downstreamWorkerGroup;

    public DownstreamChannelFactory(
            final BrokerMap brokerMap,
            final String kafkaHost,
            final int kafkaPort,
            final KafkaSslConfig kafkaSslConfig,
            final EventLoopGroup downstreamWorkerGroup
    ) {
        this.brokerMap = brokerMap;
        this.kafkaHost = kafkaHost;
        this.kafkaPort = kafkaPort;
        this.kafkaSslConfig = kafkaSslConfig;
        this.downstreamWorkerGroup = downstreamWorkerGroup;
    }

    @Override
    public ForwardChannel create(
            final UpstreamCertificateSupplier certificateSupplier,
            final Consumer<ByteBuf> upstreamSink,
            final Runnable downstreamClosedCallback
    ) {
        final ResponseRewriter rewriter = new CompositeRewriter(asList(
                new MetadataRewriter(brokerMap),
                new FindCoordinatorRewriter(brokerMap)
        ));
        final KafkaRequestStore requestStore = new KafkaRequestStore(rewriter);
        final KafkaResponseProcessor responseProcessor = new KafkaResponseProcessor(upstreamSink, requestStore);
        final KafkaMessageSplitter responseStreamSplitter = new KafkaMessageSplitter(
                responseProcessor::onResponse
        );
        final DownstreamClient downstreamClient = new DownstreamClient(
                kafkaHost,
                kafkaPort,
                kafkaSslConfig,
                responseStreamSplitter::onBytesReceived,
                downstreamClosedCallback,
                downstreamWorkerGroup
        );
        final KafkaRequestProcessor requestProcessor = new KafkaRequestProcessor(
                downstreamClient::send,
                requestStore
        );
        final KafkaMessageSplitter splitter = new KafkaMessageSplitter(requestProcessor::onRequest);
        return new ForwardChannel() {
            @Override
            public ChannelFuture close() {
                return downstreamClient.close();
            }

            @Override
            public void accept(final ByteBuf byteBuf) {
                splitter.onBytesReceived(byteBuf);
            }
        };
    }
}
