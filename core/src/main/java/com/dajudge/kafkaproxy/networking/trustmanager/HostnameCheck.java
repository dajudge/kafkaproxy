package com.dajudge.kafkaproxy.networking.trustmanager;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface HostnameCheck {
    void verify(X509Certificate cert) throws CertificateException;

    HostnameCheck NULL_VERIFIER = (cert) -> {
    };
}
