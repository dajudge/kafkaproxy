/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.kafkaproxy.ca.selfsign;

import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import static java.util.UUID.randomUUID;

public class SelfSignCertificateAuthority implements CertificateAuthority {
    private static final Logger LOG = LoggerFactory.getLogger(SelfSignCertificateAuthority.class);

    private final String algorithm;
    private final String issuerDn;
    private final PrivateKey caKeyPair;

    public SelfSignCertificateAuthority(
            final SelfSignConfig config
    ) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        this.algorithm = config.getSignatureAlgorithm();
        final Supplier<InputStream> keyStoreFile = config.getKeyStore();
        final KeyStore keyStore = KeyStore.getInstance("jks");
        final String alias = config.getKeyAlias();
        this.issuerDn = config.getIssuerDn();
        try (final InputStream data = keyStoreFile.get()) {
            keyStore.load(data, config.getKeyStorePassword());
        }
        this.caKeyPair = loadKey(keyStore, alias, config.getKeyPassword());
    }

    @Override
    public KeyStoreWrapper createClientCertificate(
            final UpstreamCertificateSupplier certificateSupplier
    ) throws SSLPeerUnverifiedException {
        final String keyPassword = randomUUID().toString();
        return new KeyStoreWrapper(
                createProxyCertificate(issuerDn, certificateSupplier.get(), algorithm, caKeyPair, keyPassword),
                keyPassword
        );
    }

    private static KeyStore createProxyCertificate(
            final String issuerDn,
            final X509Certificate client,
            final String algorithm,
            final PrivateKey caKey,
            final String keyPassword
    ) {
        try {
            LOG.info("Creating impostor certificate for \"{}\"...", client.getSubjectDN().getName());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(null, null);
            final KeyPair keyPair = Helpers.keyPair();
            final X509Certificate cert = Helpers.sign(
                    client.getSubjectDN().getName(),
                    issuerDn,
                    caKey,
                    algorithm,
                    keyPair.getPublic(),
                    client.getNotBefore(),
                    client.getNotAfter()
            );
            keystore.setKeyEntry("key", keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert});
            return keystore;
        } catch (final KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to create proxy client-certificate", e);
        }
    }

    private static PrivateKey loadKey(final KeyStore keyStore, final String alias, final char[] keyPassword) {
        try {
            return (PrivateKey) keyStore.getKey(alias, keyPassword);
        } catch (final UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to recover key from keystore", e);
        }
    }
}
