package com.dajudge.kafkaproxy.roundtrip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class SingleRoundtrip implements AbortCondition {
    private static final Logger LOG = LoggerFactory.getLogger(SingleRoundtrip.class);
    private final AtomicInteger successCounter = new AtomicInteger();
    private final long startTime = System.currentTimeMillis();
    private final long timeoutMsecs;

    public SingleRoundtrip(final long timeoutMsecs) {
        this.timeoutMsecs = timeoutMsecs;
    }

    @Override
    public boolean check(final int sent, final int completed, final int inflight, final int messagesUnknown) {
        if (completed > 0) {
            LOG.info("Roundtrip completed.");
            successCounter.incrementAndGet();
            return true;
        }
        final long now = System.currentTimeMillis();
        if ((now - startTime) > timeoutMsecs) {
            LOG.error("Timeout waiting for roundtrip.");
            return true;
        }
        return false;
    }

    public boolean completed() {
        return successCounter.get() > 0;
    }
}
