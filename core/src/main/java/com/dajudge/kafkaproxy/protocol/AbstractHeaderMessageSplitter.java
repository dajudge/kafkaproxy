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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHeaderMessageSplitter<T> implements Sink<ByteBuf> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHeaderMessageSplitter.class);
    private final Sink<T> requestSink;
    private T currentRequest = createEmptyRequest();

    protected AbstractHeaderMessageSplitter(final Sink<T> requestSink) {
        this.requestSink = requestSink;
    }

    protected abstract T createEmptyRequest();

    protected abstract void append(final T request, final ByteBuf remainingBytes);

    protected abstract boolean isComplete(final T request);

    @Override
    public void accept(final ByteBuf remainingBytes) {
        try {
            LOG.trace("Processing {} available bytes.", remainingBytes.readableBytes());
            while (remainingBytes.readableBytes() > 0) {
                LOG.trace("Processing {} bytes remaining.", remainingBytes.readableBytes());
                append(currentRequest, remainingBytes);
                if (isComplete(currentRequest)) {
                    requestSink.accept(currentRequest);
                    currentRequest = createEmptyRequest();
                }
            }
        } finally {
            remainingBytes.release();
        }
    }

    @Override
    public ChannelFuture close() {
        return requestSink.close();
    }
}
