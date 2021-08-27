/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

import com.dajudge.proxybase.AbstractChunkedMessage;
import io.netty.buffer.ByteBuf;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

import static io.netty.buffer.Unpooled.buffer;
import static java.util.Arrays.asList;
import static org.apache.kafka.common.requests.AbstractResponse.parseResponse;

public class KafkaMessage extends AbstractChunkedMessage {
    private static final int KAFKA_HEADER_LENGTH = 4;
    private static final int COMPLETE_CHUNK_COUNT = 2;

    public KafkaMessage() {
        super(KAFKA_HEADER_LENGTH);
    }

    public KafkaMessage(final ByteBuf payload) {
        super(asList(lengthHeader(payload.readableBytes()), payload));
    }

    private static ByteBuf lengthHeader(final int payloadBytes) {
        final ByteBuf buffer = buffer(4);
        buffer.writeInt(payloadBytes);
        return buffer;
    }

    @Override
    protected int nextChunkSize(final List<ByteBuf> list) {
        if (list.size() == COMPLETE_CHUNK_COUNT) {
            return NO_MORE_CHUNKS;
        }
        final ByteBuf header = list.get(0);
        try {
            return header.readInt();
        } finally {
            header.resetReaderIndex();
        }
    }

    public int correlationId() {
        return withPayload(ByteBuf::readInt);
    }

    public RequestHeader requestHeader() {
        return withPayload(payload -> RequestHeader.parse(payload.nioBuffer()));
    }

    public ResponseHeader responseHeader(final RequestHeader requestHeader) {
        final short responseHeaderVersion = requestHeader.apiKey().responseHeaderVersion(requestHeader.apiVersion());
        return withPayload(payload -> ResponseHeader.parse(payload.nioBuffer(), responseHeaderVersion));
    }

    public <T extends AbstractResponse> T responseBody(final RequestHeader requestHeader) {
        final short apiVersion = requestHeader.apiVersion();
        final ApiKeys apiKey = requestHeader.apiKey();
        return withPayload(payload -> {
            final ByteBuffer nioBuffer = payload.nioBuffer();
            ResponseHeader.parse(nioBuffer, apiKey.responseHeaderVersion(apiVersion)); // Skip over header
            // It's up to the caller to know what this message contains
            @SuppressWarnings("unchecked") final T response = (T) parseResponse(apiKey, nioBuffer, apiVersion);
            return response;
        });
    }

    private <T> T withPayload(final Function<ByteBuf, T> f) {
        final ByteBuf payload = getChunks().get(1);
        try {
            return f.apply(payload);
        } finally {
            payload.resetReaderIndex();
        }
    }
}
