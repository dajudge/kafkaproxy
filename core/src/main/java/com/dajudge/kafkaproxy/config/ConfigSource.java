package com.dajudge.kafkaproxy.config;

public interface ConfigSource<T> {
    String PREFIX = "KAFKAPROXY_";

    Class<T> getConfigClass();

    T parse(final Environment environment);
}
