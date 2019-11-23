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

package com.dajudge.kafkaproxy.ca.selfsign;

import com.dajudge.kafkaproxy.ca.ProxyClientCertificateAuthorityFactory;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateAuthorityFactory implements ProxyClientCertificateAuthorityFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CertificateAuthorityFactory.class);

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

    @Override
    public String getName() {
        return "selfsign";
    }

    @Override
    public CertificateAuthority createFactory(
            final ApplicationConfig appConfig,
            final String keyPassword
    ) {
        try {
            final SelfSignConfig config = appConfig.get(SelfSignConfig.class);
            final String algorithm = config.getSignatureAlgorithm();
            final FileResource keyStoreFile = config.getKeyStore();
            final KeyStore keyStore = KeyStore.getInstance("jks");
            final String alias = config.getKeyAlias();
            final String issuerDn = config.getIssuerDn();
            try (final InputStream data = keyStoreFile.open()) {
                keyStore.load(data, config.getKeyStorePassword());
            }
            final PrivateKey caKeyPair = loadKey(keyStore, alias, config.getKeyPassword());
            return client -> createProxyCertificate(issuerDn, client.get(), algorithm, caKeyPair, keyPassword);
        } catch (final KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to initialize self-signing proxy client-certificate factory", e);
        }
    }

    private PrivateKey loadKey(final KeyStore keyStore, final String alias, final char[] keyPassword) {
        try {
            return (PrivateKey) keyStore.getKey(alias, keyPassword);
        } catch (final UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to recover key from keystore", e);
        }
    }
}
