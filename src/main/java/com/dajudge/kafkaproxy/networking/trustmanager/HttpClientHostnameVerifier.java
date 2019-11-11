package com.dajudge.kafkaproxy.networking.trustmanager;

import org.apache.hc.client5.http.ssl.HttpsSupport;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpClientHostnameVerifier implements HostnameVerifier {
    public static final javax.net.ssl.HostnameVerifier VERIFIER = HttpsSupport.getDefaultHostnameVerifier();
    private final String hostname;

    public HttpClientHostnameVerifier(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void verify(final X509Certificate cert) throws CertificateException {
        if (!VERIFIER.verify(hostname, new DummySslSession(cert))) {
            throw new CertificateException("Certificate does not match hostname: " + hostname);
        }
    }

}
