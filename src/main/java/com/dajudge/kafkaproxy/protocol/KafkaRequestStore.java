package com.dajudge.kafkaproxy.protocol;

import com.dajudge.kafkaproxy.protocol.rewrite.ResponseRewriter;
import io.netty.buffer.ByteBuf;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.synchronizedMap;

public class KafkaRequestStore {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaRequestStore.class);
    private final Map<Integer, RequestHeader> requests = synchronizedMap(new HashMap<>());
    private final ResponseRewriter rewriter;

    public KafkaRequestStore(final ResponseRewriter rewriter) {
        this.rewriter = rewriter;
    }

    public void add(final RequestHeader requestHeader) {
        LOG.trace("Add client request: {}", requestHeader);
        requests.put(requestHeader.correlationId(), requestHeader);
    }

    public void process(final KafkaMessage response, final Consumer<ByteBuf> sink) {
        final ByteBuffer responseBuffer = response.payload().nioBuffer();
        final ResponseHeader responseHeader = ResponseHeader.parse(responseBuffer);
        final int correlationId = responseHeader.correlationId();
        final RequestHeader requestHeader = requests.get(correlationId);
        if (requestHeader == null) {
            LOG.warn("Failed to correlate response with correlationId {}", correlationId);
            sink.accept(response.serialize());
        } else {
            LOG.trace("Mapped response with correlationId {} for {}", correlationId, requestHeader);
            sink.accept(rewriter.rewrite(requestHeader, responseHeader, responseBuffer).orElse(response.serialize()));
        }
    }
}
