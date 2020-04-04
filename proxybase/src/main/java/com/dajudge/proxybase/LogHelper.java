package com.dajudge.proxybase;

import org.slf4j.MDC;

final class LogHelper {
    private static final String MDC_CHANNEL_ID = "CHANNEL_ID";

    private LogHelper() {
    }

    static void withChannelId(final String channelId, final Runnable runnable) {
        MDC.put(MDC_CHANNEL_ID, channelId);
        try {
            runnable.run();
        } finally {
            MDC.remove(MDC_CHANNEL_ID);
        }
    }
}
