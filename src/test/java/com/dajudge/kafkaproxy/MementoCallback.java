package com.dajudge.kafkaproxy;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;

class MementoCallback implements Callback {
    private boolean completed;

    @Override
    public void onCompletion(final RecordMetadata metadata, final Exception exception) {
        completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }
}
