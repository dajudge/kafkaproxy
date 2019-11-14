package com.dajudge.kafkaproxy.config.kafkassl;

import com.dajudge.kafkaproxy.config.ConfigSource;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;

public class KafkaSslConfigSource implements ConfigSource<KafkaSslConfig> {
    private static final String KAFKA_SSL_PREFIX = PREFIX + "KAFKA_SSL_";
    private static final String ENV_KAFKA_SSL_ENABLED = KAFKA_SSL_PREFIX + "ENABLED";
    private static final String ENV_KAFKA_SSL_TRUSTSTORE_LOCATION = KAFKA_SSL_PREFIX + "TRUSTSTORE_LOCATION";
    private static final String ENV_KAFKA_SSL_TRUSTSTORE_PASSWORD = KAFKA_SSL_PREFIX + "TRUSTSTORE_PASSWORD";
    private static final String ENV_KAFKA_SSL_VERIFY_HOSTNAME = KAFKA_SSL_PREFIX + "VERIFY_HOSTNAME";
    private static final boolean DEFAULT_KAFKA_SSL_ENABLED = false;
    private static final boolean DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME = true;

    @Override
    public Class<KafkaSslConfig> getConfigClass() {
        return KafkaSslConfig.class;
    }

    @Override
    public KafkaSslConfig parse(final Environment environment) {
        if (!environment.requiredBoolean(ENV_KAFKA_SSL_ENABLED, DEFAULT_KAFKA_SSL_ENABLED)) {
            return KafkaSslConfig.DISABLED;
        }
        return new KafkaSslConfig(
                environment.requiredBoolean(ENV_KAFKA_SSL_ENABLED, DEFAULT_KAFKA_SSL_ENABLED),
                environment.optionalFile(ENV_KAFKA_SSL_TRUSTSTORE_LOCATION).orElse(null),
                environment.optionalString(ENV_KAFKA_SSL_TRUSTSTORE_PASSWORD).orElse(null),
                environment.requiredBoolean(ENV_KAFKA_SSL_VERIFY_HOSTNAME, DEFAULT_KAFKA_SSL_VERIFY_HOSTNAME)
        );
    }
}
