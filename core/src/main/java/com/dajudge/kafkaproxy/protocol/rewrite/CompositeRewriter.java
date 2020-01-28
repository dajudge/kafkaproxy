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

package com.dajudge.kafkaproxy.protocol.rewrite;

import io.netty.buffer.ByteBuf;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

public class CompositeRewriter implements ResponseRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeRewriter.class);
    private final List<ResponseRewriter> rewriters;

    public CompositeRewriter(final List<ResponseRewriter> rewriters) {
        this.rewriters = unmodifiableList(rewriters);
    }

    @Override
    public boolean appliesTo(final RequestHeader requestHeader) {
        return rewriters.stream().anyMatch(it -> it.appliesTo(requestHeader));
    }

    @Override
    public Optional<ByteBuf> rewrite(
            final RequestHeader requestHeader,
            final ResponseHeader responseHeader,
            final ByteBuffer responseBuffer
    ) {
        for (final ResponseRewriter rewriter : rewriters) {
            if (rewriter.appliesTo(requestHeader)) {
                LOG.trace("Rewriting {} response", requestHeader.apiKey());
                return rewriter.rewrite(requestHeader, responseHeader, responseBuffer);
            }
        }
        LOG.trace("Passing through unmodified {} response", requestHeader.apiKey());
        return Optional.empty();
    }
}
