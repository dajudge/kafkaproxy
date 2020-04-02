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

package com.dajudge.kafkaproxy.protocol;

import com.dajudge.proxybase.Sink;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;

public class KafkaResponseProcessor implements Sink<KafkaMessage> {
    private final Sink<ByteBuf> sink;
    private final KafkaRequestStore requestStore;

    public KafkaResponseProcessor(final Sink<ByteBuf> sink, final KafkaRequestStore requestStore) {
        this.sink = sink;
        this.requestStore = requestStore;
    }

    @Override
    public void accept(final KafkaMessage response) {
        requestStore.process(response, sink);
    }

    @Override
    public ChannelFuture close() {
        return sink.close();
    }
}
