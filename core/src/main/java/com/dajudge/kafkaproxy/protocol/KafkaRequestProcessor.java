package com.dajudge.kafkaproxy.protocol;

import com.dajudge.kafkaproxy.protocol.KafkaMessage;
import com.dajudge.kafkaproxy.protocol.KafkaRequestStore;
import io.netty.buffer.ByteBuf;
import org.apache.kafka.common.requests.RequestHeader;

import java.util.function.Consumer;

public class KafkaRequestProcessor {
    private final Consumer<ByteBuf> requestSink;
    private final KafkaRequestStore kafkaRequestStore;

    public KafkaRequestProcessor(final Consumer<ByteBuf> requestSink, final KafkaRequestStore kafkaRequestStore) {
        this.requestSink = requestSink;
        this.kafkaRequestStore = kafkaRequestStore;
    }

    public void onRequest(final KafkaMessage request) {
        try {
            kafkaRequestStore.add(RequestHeader.parse(request.payload().nioBuffer()));
            requestSink.accept(request.serialize());
        } finally {
            request.release();
        }
    }
}
