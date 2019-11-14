package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertNull;

abstract class BaseOptionalConfigTest<C> extends BaseConfigTest<C> {
    @Test
    public void returns_disabled() {
        assertDisabled(disable(new TestEnvironment()));
    }

    @Test
    public void default_is_disabled() {
        assertDisabled(new TestEnvironment());
    }

    abstract TestEnvironment disable(TestEnvironment testEnvironment);

    abstract void assertDisabled(Environment env);
}
