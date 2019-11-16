package com.dajudge.kafkaproxy.common.ssl;

import com.dajudge.kafkaproxy.config.FileResource;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class DefaultTrustManagerFactory {
    public static TrustManager[] createTrustManagers(final FileResource trustStore, final char[] trustStorePassword) {
        try (final InputStream inputStream = trustStore.open()) {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(inputStream, trustStorePassword);
            factory.init(keystore);
            return factory.getTrustManagers();
        } catch (final NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new RuntimeException("Failed to setup trust manager", e);
        }
    }
}
