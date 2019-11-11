package com.dajudge.kafkaproxy.networking.downstream;

import java.io.File;

public class KafkaSslConfig {
    private final boolean sslEnabled;
    private final File trustStore;
    private final String trustStorePassword;
    private final boolean hostnameVerificationEnabled;

    public KafkaSslConfig(
            final boolean sslEnabled,
            final File trustStore,
            final String trustStorePassword,
            final boolean hostnameVerificationEnabled
    ) {
        this.sslEnabled = sslEnabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    public File getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public boolean isHostnameVerificationEnabled() {
        return hostnameVerificationEnabled;
    }
}
