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

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static java.lang.Math.min;

public class KafkaMessage {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessage.class);
    private Integer messageLength;
    private ByteBuf messageLengthBuffer = wrappedBuffer(new byte[4]).resetWriterIndex();
    private List<ByteBuf> buffers = new ArrayList<>();

    public void append(final ByteBuf remainingBytes) {
        if (messageLength == null) {
            LOG.trace(
                    "Length not present, yet. {} bytes in length buffer, {} currently available.",
                    messageLengthBuffer.writableBytes(),
                    remainingBytes.readableBytes()
            );
            final int bytesToRead = min(messageLengthBuffer.writableBytes(), remainingBytes.readableBytes());
            LOG.trace("Reading {} bytes into length buffer...", bytesToRead);
            messageLengthBuffer.writeBytes(remainingBytes, bytesToRead);
            if (messageLengthBuffer.writableBytes() > 0) {
                LOG.trace("Length still missing {} bytes. Postponing...", messageLengthBuffer.writableBytes());
                return;
            }
            messageLength = messageLengthBuffer.readInt();
            LOG.trace("Length: {}", messageLength);
        }
        final int missingBytes = missingBytes();
        LOG.trace("Now {} bytes remaining in buffer.", remainingBytes.readableBytes());
        final int bytesToCopy = min(missingBytes, remainingBytes.readableBytes());
        LOG.trace("Reading {} of {} missing bytes.", bytesToCopy, missingBytes);
        final ByteBuf copy = wrappedBuffer(new byte[bytesToCopy]).resetWriterIndex();
        copy.writeBytes(remainingBytes, bytesToCopy);
        buffers.add(copy);
    }

    private int availableBytes() {
        return buffers.stream().map(ByteBuf::readableBytes).reduce(Integer::sum).orElse(0);
    }

    private int missingBytes() {
        return length() - availableBytes();
    }

    public boolean isComplete() {
        return messageLength != null && missingBytes() == 0;
    }

    public int length() {
        if (messageLength == null) {
            throw new IllegalArgumentException("Cannot determine missing bytes when length was not read, yet");
        }
        return messageLength;
    }

    public ByteBuf serialize() {
        final ByteBuf ret = wrappedBuffer(new byte[4 + length()]).resetWriterIndex();
        ret.writeInt(length());
        ret.writeBytes(payload());
        return ret;
    }

    public ByteBuf payload() {
        return wrappedBuffer(buffers.toArray(new ByteBuf[buffers.size()]));
    }

    public void release() {
        buffers.forEach(ByteBuf::release);
    }
}
