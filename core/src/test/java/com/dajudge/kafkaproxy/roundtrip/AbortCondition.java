package com.dajudge.kafkaproxy.roundtrip;

public interface AbortCondition {
    boolean check(final int sent, final int completed, final int inflight, final int messagesUnknown);
}
