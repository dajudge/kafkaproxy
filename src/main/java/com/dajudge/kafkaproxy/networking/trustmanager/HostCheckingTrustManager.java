package com.dajudge.kafkaproxy.networking.trustmanager;

import com.dajudge.kafkaproxy.networking.trustmanager.HostnameVerifier;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

public class HostCheckingTrustManager implements X509TrustManager {
    private final Collection<X509TrustManager> nextManagers;
    private final HostnameVerifier hostnameVerifier;

    public HostCheckingTrustManager(
            final Collection<X509TrustManager> nextManagers,
            final HostnameVerifier hostnameVerifier
    ) {
        this.nextManagers = nextManagers;
        this.hostnameVerifier = hostnameVerifier;
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
        hostnameVerifier.verify(chain[0]);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
