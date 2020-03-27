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

package com.dajudge.kafkaproxy.protocol;

import com.dajudge.kafkaproxy.networking.upstream.ForwardChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMessageSplitter implements ForwardChannel<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageSplitter.class);
    private final ForwardChannel<KafkaMessage> requestSink;
    private KafkaMessage currentRequest = new KafkaMessage();

    public KafkaMessageSplitter(final ForwardChannel<KafkaMessage> requestSink) {
        this.requestSink = requestSink;
    }

    @Override
    public void accept(final ByteBuf remainingBytes) {
        LOG.trace("Processing {} available bytes.", remainingBytes.readableBytes());
        while (remainingBytes.readableBytes() > 0) {
            LOG.trace("Processing {} bytes remaining.", remainingBytes.readableBytes());
            currentRequest.append(remainingBytes);
            if (currentRequest.isComplete()) {
                requestSink.accept(currentRequest);
                currentRequest = new KafkaMessage();
            }
        }
    }

    @Override
    public ChannelFuture close() {
        return requestSink.close();
    }
}
