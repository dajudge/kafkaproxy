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

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.config.proxyssl.ProxySslConfigSource;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;
import com.dajudge.kafkaproxy.util.environment.TestEnvironment;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class ProxySslConfigTest extends BaseOptionalConfigTest<ProxySslConfig> {

    @Test
    public void accepts_full_config() {
        final ProxySslConfig config = parse(fullEnvironment());
        assertTrue(config.isEnabled());
        assertEquals("truststore", Util.toString(config.getTrustStore()));
        assertEquals("truststorePassword", config.getTrustStorePassword());
        assertEquals("keystore", Util.toString(config.getKeyStore()));
        assertEquals("keystorePassword", config.getKeyStorePassword());
        assertEquals("keyPassword", config.getKeyPassword());
    }

    @Test
    public void accepts_unset_keystore_location() {
        assertAllowsUnset("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", ProxySslConfig::getKeyStore);
    }

    @Test
    public void accepts_unset_keystore_password() {
        assertAllowsUnset("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", ProxySslConfig::getKeyStorePassword);
    }

    @Test
    public void accepts_unset_key_password() {
        assertAllowsUnset("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", ProxySslConfig::getKeyPassword);
    }

    @Test
    public void accepts_unset_truststore_location() {
        assertAllowsUnset("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", ProxySslConfig::getTrustStore);
    }

    @Test
    public void accepts_unset_truststore_password() {
        assertAllowsUnset("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", ProxySslConfig::getTrustStorePassword);
    }

    @Override
    TestEnvironment fullEnvironment() {
        return new TestEnvironment()
                .withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "true")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_LOCATION", "truststore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_TRUSTSTORE_PASSWORD", "truststorePassword")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_LOCATION", "keystore.jks")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEYSTORE_PASSWORD", "keystorePassword")
                .withEnv("KAFKAPROXY_CLIENT_SSL_KEY_PASSWORD", "keyPassword")
                .withFile("truststore.jks", "truststore".getBytes(UTF_8))
                .withFile("keystore.jks", "keystore".getBytes(UTF_8));
    }

    @Override
    void assertDisabled(final Environment env) {
        final ProxySslConfig config = parse(env);
        assertFalse(config.isEnabled());
        assertNull(config.getTrustStore());
        assertNull(config.getTrustStorePassword());
        assertNull(config.getKeyStore());
        assertNull(config.getKeyStorePassword());
        assertNull(config.getKeyPassword());
    }

    @Override
    ProxySslConfig parse(final Environment env) {
        return new ProxySslConfigSource().parse(env);
    }

    @Override
    TestEnvironment disable(final TestEnvironment testEnvironment) {
        return testEnvironment
                .withEnv("KAFKAPROXY_CLIENT_SSL_ENABLED", "false");
    }
}
