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

import com.dajudge.kafkaproxy.protocol.rewrite.ResponseRewriter;
import org.apache.kafka.common.requests.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;

public class KafkaRequestStore {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaRequestStore.class);
    private final Map<Integer, RequestHeader> requests = synchronizedMap(new HashMap<>());
    private final ResponseRewriter rewriter;

    public KafkaRequestStore(final ResponseRewriter rewriter) {
        this.rewriter = rewriter;
    }

    public void add(final KafkaMessage request) {
        final RequestHeader requestHeader = request.requestHeader();
        if (LOG.isDebugEnabled()) {
            LOG.trace("Adding client request: {} (inflight {}) ", requestHeader, requests.keySet());
        }
        requests.put(requestHeader.correlationId(), requestHeader);
    }

    public KafkaMessage process(final KafkaMessage response) {
        // Peek at the correlation ID
        final int correlationId = response.correlationId();
        if (LOG.isDebugEnabled()) {
            LOG.trace(
                    "Processing response with correlation ID {} (inflight: {})",
                    correlationId,
                    requests.keySet()
            );
        }
        final RequestHeader requestHeader = requests.remove(correlationId);
        if (requestHeader == null) {
            throw new RuntimeException(format(
                    "Failed to correlate response correlation ID %d",
                    correlationId
            ));
        }

        return rewriter.rewrite(requestHeader, response);
    }
}
