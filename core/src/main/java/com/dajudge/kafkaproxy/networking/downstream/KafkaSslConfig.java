package com.dajudge.kafkaproxy.networking.downstream;

import com.dajudge.kafkaproxy.config.FileResource;

public class KafkaSslConfig {
    private final boolean enabled;
    private final FileResource trustStore;
    private final String trustStorePassword;
    private final boolean hostnameVerificationEnabled;

    public static KafkaSslConfig DISABLED = new KafkaSslConfig(false, null, null, false);

    public KafkaSslConfig(
            final boolean enabled,
            final FileResource trustStore,
            final String trustStorePassword,
            final boolean hostnameVerificationEnabled
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    public FileResource getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHostnameVerificationEnabled() {
        return hostnameVerificationEnabled;
    }
}
