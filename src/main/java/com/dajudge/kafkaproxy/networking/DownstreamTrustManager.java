package com.dajudge.kafkaproxy.networking;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class DownstreamTrustManager implements TrustManager {
    public static TrustManager[] createTrustManagers(final File trustStore, final char[] trustStorePassword) {
        try (final InputStream inputStream = new FileInputStream(trustStore)) {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(inputStream, trustStorePassword);
            factory.init(keystore);
            return factory.getTrustManagers();
        } catch (final NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new RuntimeException("Failed to setup downstream trust manager", e);
        }
    }
}
