package com.dajudge.kafkaproxy.util.ssl;

import java.io.File;

public class SslTestAuthority {
    private final File caTrustStore;
    private final String password;

    public SslTestAuthority(final File caTrustStore, final String password) {
        this.caTrustStore = caTrustStore;
        this.password = password;
    }

    public String getTrustStorePassword() {
        return password;
    }

    public File getTrustStore() {
        return caTrustStore;
    }
}
