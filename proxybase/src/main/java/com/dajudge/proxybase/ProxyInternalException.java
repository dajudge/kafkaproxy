package com.dajudge.proxybase;

/**
 * This exception signals a severe error in the proxy's internal state.
 */
public class ProxyInternalException extends RuntimeException {
    public ProxyInternalException(final String message) {
        super(message);
    }

    public ProxyInternalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
