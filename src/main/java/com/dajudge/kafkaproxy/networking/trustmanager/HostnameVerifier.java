package com.dajudge.kafkaproxy.networking.trustmanager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface HostnameVerifier {
    void verify(X509Certificate cert) throws CertificateException;

    HostnameVerifier NULL_VERIFIER = (cert) -> {
    };
}
