package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.config.kafkassl.KafkaSslConfigSource;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class KafkaSslConfigTest extends BaseOptionalConfigTest<KafkaSslConfig> {
    @Test
    public void accepts_full_config() {
        final KafkaSslConfig config = parse(fullEnvironment());
        assertTrue(config.isEnabled());
        assertEquals("truststore", Util.toString(config.getTrustStore()));
        assertEquals("truststorePassword", config.getTrustStorePassword());
    }

    @Test
    public void accepts_unset_truststore_location() {
        assertAllowsUnset("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", KafkaSslConfig::getTrustStore);
    }

    @Test
    public void accepts_unset_truststore_password() {
        assertAllowsUnset("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", KafkaSslConfig::getTrustStorePassword);
    }

    @Test
    public void accepts_unset_hostname_verification() {
        final Environment env = fullEnvironment()
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", null);
        assertTrue(parse(env).isHostnameVerificationEnabled());
    }

    @Test
    public void parses_true_hostname_verification() {
        final Environment env = fullEnvironment()
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true");
        assertTrue(parse(env).isHostnameVerificationEnabled());
    }

    @Test
    public void parses_false_hostname_verification() {
        final Environment env = fullEnvironment()
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "false");
        assertFalse(parse(env).isHostnameVerificationEnabled());
    }

    @Override
    TestEnvironment fullEnvironment() {
        return new TestEnvironment()
                .withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_LOCATION", "truststore.jks")
                .withEnv("KAFKAPROXY_KAFKA_SSL_TRUSTSTORE_PASSWORD", "truststorePassword")
                .withEnv("KAFKAPROXY_KAFKA_SSL_VERIFY_HOSTNAME", "true")
                .withFile("truststore.jks", "truststore".getBytes(UTF_8));
    }

    @Override
    void assertDisabled(final Environment env) {
        final KafkaSslConfig config = parse(env);
        assertFalse(config.isEnabled());
        assertNull(config.getTrustStore());
        assertNull(config.getTrustStorePassword());
        assertFalse(config.isHostnameVerificationEnabled());
    }

    @Override
    KafkaSslConfig parse(final Environment env) {
        return new KafkaSslConfigSource().parse(env);
    }

    @Override
    TestEnvironment disable(final TestEnvironment testEnvironment) {
        return testEnvironment.withEnv("KAFKAPROXY_KAFKA_SSL_ENABLED", "false");
    }
}
