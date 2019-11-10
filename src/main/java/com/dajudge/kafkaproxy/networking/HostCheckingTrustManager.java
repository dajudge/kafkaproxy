package com.dajudge.kafkaproxy.networking;

import sun.security.util.HostnameChecker;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import static sun.security.util.HostnameChecker.TYPE_TLS;

public class HostCheckingTrustManager implements X509TrustManager {
    private final Collection<X509TrustManager> nextManagers;
    private final String hostname;
    private final boolean hostnameVerificationEnabled;

    public HostCheckingTrustManager(
            final Collection<X509TrustManager> nextManagers,
            final String hostname,
            final boolean hostnameVerificationEnabled
    ) {
        this.nextManagers = nextManagers;
        this.hostname = hostname;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        throw new CertificateException("Cannot check client certificate");
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        for (final X509TrustManager nextManager : nextManagers) {
            nextManager.checkServerTrusted(chain, authType);
        }
        if (hostnameVerificationEnabled) {
            HostnameChecker.getInstance(TYPE_TLS).match(hostname, chain[0]);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
