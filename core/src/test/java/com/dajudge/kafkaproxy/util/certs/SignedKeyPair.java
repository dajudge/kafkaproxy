package com.dajudge.kafkaproxy.util.certs;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static com.dajudge.kafkaproxy.ca.Helpers.createJks;


public class SignedKeyPair {
    private final KeyPair keyPair;
    private final X509Certificate cert;

    public SignedKeyPair(final KeyPair keyPair, final X509Certificate cert) {
        this.keyPair = keyPair;
        this.cert = cert;
    }

    public byte[] toKeyStore(final String keystorePassword, final String keyPassword) {
        return createJks(keystorePassword, keystore -> {
            keystore.setKeyEntry("key", keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert});
        });
    }
}
