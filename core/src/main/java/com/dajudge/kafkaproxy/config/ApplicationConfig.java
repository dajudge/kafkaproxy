package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.config.broker.BrokerConfig;
import com.dajudge.kafkaproxy.config.broker.BrokerConfigSource;
import com.dajudge.kafkaproxy.config.kafkassl.KafkaSslConfigSource;
import com.dajudge.kafkaproxy.config.proxyssl.ProxySslConfigSource;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class ApplicationConfig {
    private final Environment environment;
    private final Map<Class<?>, ConfigSource<?>> sources = unmodifiableMap(new HashMap<Class<?>, ConfigSource<?>>() {{
        put(BrokerConfig.class, new BrokerConfigSource());
        put(ProxySslConfig.class, new ProxySslConfigSource());
        put(KafkaSslConfig.class, new KafkaSslConfigSource());
    }});

    public ApplicationConfig(final Environment environment) {
        this.environment = environment;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final Class<T> configClass) {
        return (T) sources.get(configClass).parse(environment);
    }
}
