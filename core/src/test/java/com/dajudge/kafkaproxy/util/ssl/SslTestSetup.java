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

package com.dajudge.kafkaproxy.util.ssl;

import com.dajudge.kafkaproxy.util.certs.CertificateAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Locale.US;

public class SslTestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(SslTestSetup.class);
    private static final Random RANDOM = new Random();
    private final Map<String, SslTestKeystore> brokerKeyStores;
    private final SslTestAuthority sslTestAuthority;

    public SslTestSetup(
            final Map<String, SslTestKeystore> brokerKeyStores,
            final SslTestAuthority sslTestAuthority
    ) {
        this.brokerKeyStores = brokerKeyStores;
        this.sslTestAuthority = sslTestAuthority;
    }

    public SslTestKeystore getBroker(final String name) {
        return brokerKeyStores.get(name);
    }

    public SslTestAuthority getAuthority() {
        return sslTestAuthority;
    }

    public interface Builder {
        SslTestSetup build();

        Builder withBrokers(Collection<String> collect);
    }

    public static Builder sslSetup(final String dn, final File basePath) {
        final Map<String, SslTestKeystore> brokerKeyStores = new HashMap<>();
        final CertificateAuthority ca = CertificateAuthority.create(dn);
        return new Builder() {
            @Override
            public SslTestSetup build() {
                final String password = randomPassword();
                final File passwordFile = write(basePath, dn + ".trustStorePwd", password);
                final File caTrustStore = write(basePath, "ca.jks", ca.toTrustStore(password));
                LOG.info("Wrote truststore for CA \"{}\" to {}", dn, caTrustStore.getAbsolutePath());
                return new SslTestSetup(brokerKeyStores, new SslTestAuthority(caTrustStore, password, passwordFile));
            }

            @Override
            public Builder withBrokers(final Collection<String> brokers) {
                brokers.forEach(broker -> {
                    final String dn = "CN=" + broker;
                    final String keyStorePassword = randomPassword();
                    final String keyPassword = randomPassword();
                    final byte[] jks = ca.createAndSignKeyPair(dn)
                            .toKeyStore(keyStorePassword, keyPassword);
                    final File jksFile = write(basePath, broker + ".jks", jks);
                    final File keyPasswordFile = write(basePath, broker + ".keyPwd", keyPassword);
                    final File keyStorePasswordFile = write(basePath, broker + ".keyStorePwd", keyStorePassword);
                    LOG.info("Wrote keystore for \"{}\" to {}", dn, jksFile.getAbsolutePath());
                    brokerKeyStores.put(broker, new SslTestKeystore(
                            jksFile,
                            keyStorePassword,
                            keyStorePasswordFile,
                            keyPassword,
                            keyPasswordFile
                    ));
                });
                return this;
            }
        };
    }

    private static File write(final File basePath, final String filename, final String string) {
        return write(basePath, filename, string.getBytes(ISO_8859_1));
    }

    private static File write(final File basePath, final String storeName, final byte[] jks) {
        if (!basePath.exists() && !basePath.mkdirs()) {
            throw new RuntimeException("Could not create directory " + basePath.getAbsolutePath());

        }
        final File outFile = new File(basePath, storeName);
        try (final FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(jks);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return outFile;
    }

    private static String randomPassword() {
        final StringBuilder builder = new StringBuilder();
        final String alphabet = "abcdefghijklmnopqrstuvwxyz";
        final String numbers = "0123456789";
        final String chars = alphabet + alphabet.toUpperCase(US) + numbers;
        for (int i = 0; i < 32; i++) {
            builder.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return builder.toString();
    }
}
