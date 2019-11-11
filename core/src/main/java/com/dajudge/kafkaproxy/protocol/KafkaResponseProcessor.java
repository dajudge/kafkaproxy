package com.dajudge.kafkaproxy.protocol;

import io.netty.buffer.ByteBuf;

import java.util.function.Consumer;

public class KafkaResponseProcessor {
    private final Consumer<ByteBuf> sink;
    private final KafkaRequestStore requestStore;

    public KafkaResponseProcessor(final Consumer<ByteBuf> sink, final KafkaRequestStore requestStore) {
        this.sink = sink;
        this.requestStore = requestStore;
    }

    public void onResponse(final KafkaMessage response) {
        requestStore.process(response, sink);
    }
}
