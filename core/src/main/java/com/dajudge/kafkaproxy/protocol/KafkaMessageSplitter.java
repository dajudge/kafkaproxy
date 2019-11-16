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

package com.dajudge.kafkaproxy.protocol;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class KafkaMessageSplitter {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageSplitter.class);
    private final Consumer<KafkaMessage> requestSink;
    private KafkaMessage currentRequest = new KafkaMessage();

    public KafkaMessageSplitter(final Consumer<KafkaMessage> requestSink) {
        this.requestSink = requestSink;
    }

    public void onBytesReceived(final ByteBuf remainingBytes) {
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
}
