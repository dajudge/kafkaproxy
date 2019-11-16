/*
 * Copyright 2019 Alex Stockinger
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
