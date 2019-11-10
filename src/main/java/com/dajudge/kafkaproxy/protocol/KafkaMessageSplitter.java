package com.dajudge.kafkaproxy.protocol;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class KafkaMessageSplitter {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageSplitter.class);
    private final Consumer<KafkaMessage> requestSink;
    private KafkaMessage currentRequest = new KafkaMessage();

    public KafkaMessageSplitter(final Consumer<KafkaMessage> requestSink) {
        this.requestSink = requestSink;
    }

    public void onBytesReceived(final ByteBuf remainingBytes) {
        LOG.trace("Processing {} available bytes.", remainingBytes.readableBytes());
        while (remainingBytes.readableBytes() > 0) {
            LOG.trace("Processing {} bytes remaining.", remainingBytes.readableBytes());
            currentRequest.append(remainingBytes);
            if (currentRequest.isComplete()) {
                requestSink.accept(currentRequest);
                currentRequest = new KafkaMessage();
            }
        }
    }
}
