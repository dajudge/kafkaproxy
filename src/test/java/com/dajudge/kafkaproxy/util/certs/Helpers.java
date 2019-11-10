package com.dajudge.kafkaproxy.util.certs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.x509.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

final class Helpers {
    private static final Logger LOG = LoggerFactory.getLogger(Helpers.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Helpers() {
    }

    static <T> T call(final ThrowingCallable<T> callable) {
        try {
            return callable.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void call(final ThrowingRunnable runnable) {
        call(() -> {
            runnable.call();
            return null;
        });
    }

    static X509Certificate selfSignedCert(
            final String dn,
            final KeyPair pair,
            final int days,
            final String algorithm
    ) {
        final PublicKey publicKey = pair.getPublic();
        return sign(dn, dn, pair, days, algorithm, publicKey);
    }

    static X509Certificate sign(
            final String ownerDn,
            final String issuerDn,
            final KeyPair signingPair,
            final int days,
            final String algorithm,
            final PublicKey publicKey
    ) {
        // https://stackoverflow.com/questions/1615871/creating-an-x509-certificate-in-java-without-bouncycastle
        return call(() -> {
            final PrivateKey privkey = signingPair.getPrivate();
            final X509CertInfo info = new X509CertInfo();
            final Date from = new Date();
            final Date to = new Date(from.getTime() + days * 86400000l);
            final CertificateValidity interval = new CertificateValidity(from, to);
            final BigInteger sn = new BigInteger(64, SECURE_RANDOM);
            final X500Name owner = new X500Name(ownerDn);
            final X500Name issuer = new X500Name(issuerDn);

            info.set(X509CertInfo.VALIDITY, interval);
            info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
            info.set(X509CertInfo.SUBJECT, owner);
            info.set(X509CertInfo.ISSUER, issuer);
            info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
            info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
            info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

            // Sign the cert to identify the algorithm that's used.
            X509CertImpl cert = new X509CertImpl(info);
            cert.sign(privkey, algorithm);

            // Update the algorith, and resign.
            algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
            info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
            cert = new X509CertImpl(info);
            cert.sign(privkey, algorithm);
            return cert;
        });
    }

    static KeyPair keyPair() {
        return call(() -> {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        });
    }

    static byte[] createJks(final String password, final ThrowingConsumer<KeyStore> withKeyStore) {
        return call(() -> {
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(null, null);
            withKeyStore.accept(keystore);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keystore.store(bos, password.toCharArray());
            return bos.toByteArray();
        });
    }

    interface ThrowingCallable<T> {
        T call() throws Exception;
    }

    interface ThrowingRunnable<T> {
        void call() throws Exception;
    }

    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
