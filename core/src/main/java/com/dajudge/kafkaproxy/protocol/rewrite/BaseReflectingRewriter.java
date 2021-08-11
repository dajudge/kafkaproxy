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

package com.dajudge.kafkaproxy.protocol.rewrite;

import com.dajudge.kafkaproxy.protocol.KafkaMessage;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.RequestUtils;
import org.apache.kafka.common.requests.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.apache.kafka.common.requests.RequestUtils.serialize;

public abstract class BaseReflectingRewriter<T extends AbstractResponse> implements ResponseRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(BaseReflectingRewriter.class);

    public final KafkaMessage rewrite(
            final RequestHeader requestHeader,
            final KafkaMessage message
    ) {
        if (!appliesTo(requestHeader)) {
            LOG.trace("Not rewriting {} with rewriter {}", requestHeader, getClass().getSimpleName());
            return message;
        }
        LOG.trace("Rewriting {} with rewriter {}", requestHeader, getClass().getSimpleName());
        try {
            return rewriteMessage(requestHeader, message);
        } finally {
            message.release();
        }
    }

    private KafkaMessage rewriteMessage(final RequestHeader requestHeader, final KafkaMessage message) {
        final ResponseHeader header = message.responseHeader(requestHeader);
        final T rewrittenMessage = rewriteMessageBody(requestHeader, message);
        return new KafkaMessage(wrappedBuffer(serialize(
                header.data(),
                header.headerVersion(),
                rewrittenMessage.data(),
                requestHeader.apiVersion()
        )));

    }

    private T rewriteMessageBody(final RequestHeader requestHeader, final KafkaMessage message) {
        final T response = message.responseBody(requestHeader);
        try {
            rewrite(response);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to rewrite metadata response", e);
        }
        return response;
    }

    protected abstract boolean appliesTo(RequestHeader requestHeader);

    protected abstract void rewrite(final T response) throws NoSuchFieldException, IllegalAccessException;
}
