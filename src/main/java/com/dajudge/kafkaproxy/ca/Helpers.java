package com.dajudge.kafkaproxy.ca;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import static java.lang.System.currentTimeMillis;

public final class Helpers {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Helpers() {
    }

    private static <T> T call(final ThrowingCallable<T> callable) {
        try {
            return callable.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate selfSignedCert(
            final String dn,
            final KeyPair pair,
            final int days,
            final String algorithm
    ) {
        final PublicKey publicKey = pair.getPublic();
        return sign(dn, dn, pair, days, algorithm, publicKey);
    }

    public static X509Certificate sign(
            final String ownerDn,
            final String issuerDn,
            final KeyPair signingPair,
            final int days,
            final String algorithm,
            final PublicKey publicKey
    ) {
        try {
            final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                    .find(algorithm);
            final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
                    .find(sigAlgId);

            final X509v3CertificateBuilder certGenerator = new X509v3CertificateBuilder(
                    new X500Name(issuerDn),
                    BigInteger.valueOf(SECURE_RANDOM.nextInt()),
                    new Date(currentTimeMillis()),
                    new Date(currentTimeMillis() + days * 365 * 24 * 60 * 60 * 1000),
                    new X500Name(ownerDn),
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
            );

            final ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                    .build(PrivateKeyFactory.createKey(signingPair.getPrivate().getEncoded()));

            final X509CertificateHolder holder = certGenerator.build(sigGen);
            final Certificate eeX509CertificateStructure = holder.toASN1Structure();
            final CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

            try (final InputStream stream = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
                return (X509Certificate) cf.generateCertificate(stream);
            }
        } catch (final CertificateException | IOException | OperatorCreationException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to sign certificate", e);
        }
    }

    public static KeyPair keyPair() {
        return call(() -> {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        });
    }

    public static byte[] createJks(final String password, final ThrowingConsumer<KeyStore> withKeyStore) {
        return call(() -> {
            final KeyStore keystore = KeyStore.getInstance("jks");
            keystore.load(null, null);
            withKeyStore.accept(keystore);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keystore.store(bos, password.toCharArray());
            return bos.toByteArray();
        });
    }

    public interface ThrowingCallable<T> {
        T call() throws Exception;
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
