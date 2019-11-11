package com.dajudge.kafkaproxy.networking.trustmanager;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import java.security.Principal;
import java.security.cert.Certificate;

class DummySslSession implements SSLSession {
    private final Certificate certificate;

    DummySslSession(final Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public byte[] getId() {
        return new byte[0];
    }

    @Override
    public SSLSessionContext getSessionContext() {
        return null;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public long getLastAccessedTime() {
        return 0;
    }

    @Override
    public void invalidate() {

    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void putValue(final String name, final Object value) {

    }

    @Override
    public Object getValue(final String name) {
        return null;
    }

    @Override
    public void removeValue(final String name) {

    }

    @Override
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return new Certificate[]{certificate};
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return new Certificate[0];
    }

    @Override
    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return new javax.security.cert.X509Certificate[0];
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return null;
    }

    @Override
    public Principal getLocalPrincipal() {
        return null;
    }

    @Override
    public String getCipherSuite() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getPeerHost() {
        return null;
    }

    @Override
    public int getPeerPort() {
        return 0;
    }

    @Override
    public int getPacketBufferSize() {
        return 0;
    }

    @Override
    public int getApplicationBufferSize() {
        return 0;
    }
}
