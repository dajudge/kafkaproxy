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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Locale.US;

public class SslTestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(SslTestSetup.class);
    private static final Random RANDOM = new Random();
    private final Map<String, SslTestKeyStore> brokerKeyStores;
    private final Map<String, SslTestKeyStore> clientKeyStores;
    private final SslTestAuthority sslTestAuthority;
    private final String caName;

    public SslTestSetup(
            final Map<String, SslTestKeyStore> brokerKeyStores,
            final Map<String, SslTestKeyStore> clientKeyStores,
            final SslTestAuthority sslTestAuthority,
            final String caName
    ) {
        this.brokerKeyStores = brokerKeyStores;
        this.clientKeyStores = clientKeyStores;
        this.sslTestAuthority = sslTestAuthority;
        this.caName = caName;
    }

    public SslTestKeyStore getBroker(final String name) {
        return brokerKeyStores.get(name);
    }

    public SslTestAuthority getAuthority() {
        return sslTestAuthority;
    }

    public SslTestKeyStore getClient(final String dn) {
        return clientKeyStores.get(dn);
    }

    public String getCaName() {
        return caName;
    }

    public interface Builder {
        SslTestSetup build();

        Builder withBrokers(Collection<String> collect);

        Builder withClients(Collection<String> clients);
    }

    public static Builder sslSetup(final String dn, final File basePath) {
        final Map<String, SslTestKeyStore> brokerKeyStores = new HashMap<>();
        final Map<String, SslTestKeyStore> clientKeyStores = new HashMap<>();
        final CertificateAuthority ca = CertificateAuthority.create(dn);
        final String caKeyAlias = "caKey";
        final String keyStorePassword = randomPassword();
        final String keyPassword = randomPassword();
        final byte[] keyStore = ca.toKeyStore(caKeyAlias, keyStorePassword, keyPassword);
        final SslTestKeyStore caKeyStore = createKeyStore(
                basePath,
                dn,
                UUID.randomUUID().toString(),
                keyStorePassword,
                keyPassword,
                keyStore
        );
        return new Builder() {
            @Override
            public SslTestSetup build() {
                final String password = randomPassword();
                final String filename = UUID.randomUUID().toString();
                final File passwordFile = write(basePath, filename + ".trustStorePwd", password);
                final File caTrustStore = write(basePath, filename + ".truststore.jks", ca.toTrustStore(password));
                LOG.info("Wrote truststore for CA \"{}\" to {}", dn, caTrustStore.getAbsolutePath());
                final SslTestAuthority testCa = new SslTestAuthority(caTrustStore, password, passwordFile, caKeyStore);
                return new SslTestSetup(brokerKeyStores, clientKeyStores, testCa, dn);
            }

            @Override
            public Builder withBrokers(final Collection<String> brokers) {
                brokers.forEach(broker -> brokerKeyStores.put(broker, createKeystore("CN=" + broker)));
                return this;
            }

            @Override
            public Builder withClients(final Collection<String> clients) {
                clients.forEach(client -> clientKeyStores.put(client, createKeystore(client)));
                return this;
            }

            @NotNull
            private SslTestKeyStore createKeystore(final String dn) {
                final String filename = UUID.randomUUID().toString();
                final String keyStorePassword = randomPassword();
                final String keyPassword = randomPassword();
                final byte[] jks = ca.createAndSignKeyPair(dn)
                        .toKeyStore(keyStorePassword, keyPassword);
                return createKeyStore(basePath, dn, filename, keyStorePassword, keyPassword, jks);
            }

        };
    }

    @NotNull
    private static SslTestKeyStore createKeyStore(
            final File basePath,
            final String dn,
            final String filename,
            final String keyStorePassword,
            final String keyPassword,
            final byte[] jks
    ) {
        final File jksFile = write(basePath, filename + ".keystore.jks", jks);
        final File keyPasswordFile = write(basePath, filename + ".keyPwd", keyPassword);
        final File keyStorePasswordFile = write(basePath, filename + ".keyStorePwd", keyStorePassword);
        LOG.info("Wrote keystore for \"{}\" to {}", dn, jksFile.getAbsolutePath());
        return new SslTestKeyStore(
                jksFile,
                keyStorePassword,
                keyStorePasswordFile,
                keyPassword,
                keyPasswordFile
        );
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
