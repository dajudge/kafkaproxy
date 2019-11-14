package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;

import java.util.function.Function;

import static org.junit.Assert.assertNull;

abstract class BaseConfigTest<C> {
    abstract TestEnvironment fullEnvironment();

    <T> void assertAllowsUnset(final String envVar, final Function<C, T> configProperty) {
        final Environment env = fullEnvironment()
                .withEnv(envVar, null);
        assertNull(configProperty.apply(parse(env)));
    }

    abstract C parse(Environment e);
}
