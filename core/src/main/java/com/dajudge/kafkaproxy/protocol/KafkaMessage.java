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

import com.dajudge.proxybase.message.AbstractFixedSizeHeaderMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class KafkaMessage extends AbstractFixedSizeHeaderMessage {
    private static final int HEADER_LENGTH = 4;

    public KafkaMessage() {
        super(HEADER_LENGTH);
    }

    public ByteBuf serialize() {
        final ByteBuf ret = Unpooled.buffer(HEADER_LENGTH + length());
        ret.writeInt(length());
        ret.writeBytes(payload());
        return ret;
    }

    @Override
    protected int getPayloadLength(final ByteBuf headerBuffer) {
        return headerBuffer.readInt();
    }
}
