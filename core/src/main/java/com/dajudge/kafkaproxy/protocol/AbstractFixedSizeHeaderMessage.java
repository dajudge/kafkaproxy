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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.buffer.Unpooled.buffer;
import static java.lang.Math.min;

public abstract class AbstractFixedSizeHeaderMessage {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFixedSizeHeaderMessage.class);
    private final ByteBuf headerBuffer;
    private ByteBuf payloadBuffer;

    protected AbstractFixedSizeHeaderMessage(final int headerLength) {
        headerBuffer = Unpooled.buffer(headerLength, headerLength);
    }

    public void append(final ByteBuf remainingBytes) {
        if (payloadBuffer == null) {
            LOG.trace(
                    "Header not complete, yet. {} bytes in length buffer, {} currently available.",
                    headerBuffer.writableBytes(),
                    remainingBytes.readableBytes()
            );
            final int bytesToRead = min(headerBuffer.writableBytes(), remainingBytes.readableBytes());
            LOG.trace("Reading {} bytes into header buffer...", bytesToRead);
            headerBuffer.writeBytes(remainingBytes, bytesToRead);
            if (headerBuffer.writableBytes() > 0) {
                LOG.trace("Header still missing {} bytes. Postponing...", headerBuffer.writableBytes());
                return;
            }
            final int messageLength = getPayloadLength(headerBuffer);
            payloadBuffer = buffer(messageLength, messageLength);
            LOG.trace("Length: {}", payloadBuffer.writableBytes());
        }
        final int missingBytes = missingBytes();
        LOG.trace("Now {} bytes remaining in buffer.", remainingBytes.readableBytes());
        final int bytesToCopy = min(missingBytes, remainingBytes.readableBytes());
        LOG.trace("Reading {} of {} missing bytes.", bytesToCopy, missingBytes);
        payloadBuffer.writeBytes(remainingBytes, bytesToCopy);
    }

    protected abstract int getPayloadLength(final ByteBuf headerBuffer);

    private int availableBytes() {
        return payloadBuffer.readableBytes();
    }

    private int missingBytes() {
        return length() - availableBytes();
    }

    public boolean isComplete() {
        return payloadBuffer != null && missingBytes() == 0;
    }

    public int length() {
        if (payloadBuffer == null) {
            throw new IllegalArgumentException("Cannot determine missing bytes when length was not read, yet");
        }
        return payloadBuffer.readableBytes() + payloadBuffer.writableBytes();
    }

    public ByteBuf payload() {
        return payloadBuffer;
    }

    public void release() {
        payloadBuffer.release();
        headerBuffer.release();
    }
}
