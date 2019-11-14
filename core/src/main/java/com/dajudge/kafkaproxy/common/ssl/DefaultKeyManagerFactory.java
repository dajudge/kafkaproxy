package com.dajudge.kafkaproxy.common.ssl;

import com.dajudge.kafkaproxy.config.FileResource;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class DefaultKeyManagerFactory {
    public static KeyManager[] createKeyManagers(
            final FileResource keyStore,
            final char[] keyStorePassword,
            final char[] keyPassword
    ) {
        try (final InputStream inputStream = keyStore.open()) {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(inputStream, keyStorePassword);
            factory.init(keystore, keyPassword);
            return factory.getKeyManagers();
        } catch (final UnrecoverableKeyException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to setup key manager", e);
        }
    }
}
