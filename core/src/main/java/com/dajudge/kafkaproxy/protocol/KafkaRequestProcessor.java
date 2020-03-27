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
import org.apache.kafka.common.requests.RequestHeader;

public class KafkaRequestProcessor implements ForwardChannel<KafkaMessage> {
    private final ForwardChannel<ByteBuf> requestSink;
    private final KafkaRequestStore kafkaRequestStore;

    public KafkaRequestProcessor(final ForwardChannel<ByteBuf> requestSink, final KafkaRequestStore kafkaRequestStore) {
        this.requestSink = requestSink;
        this.kafkaRequestStore = kafkaRequestStore;
    }

    @Override
    public void accept(final KafkaMessage request) {
        try {
            kafkaRequestStore.add(RequestHeader.parse(request.payload().nioBuffer()));
            requestSink.accept(request.serialize());
        } finally {
            request.release();
        }
    }

    @Override
    public ChannelFuture close() {
        return requestSink.close();
    }
}
