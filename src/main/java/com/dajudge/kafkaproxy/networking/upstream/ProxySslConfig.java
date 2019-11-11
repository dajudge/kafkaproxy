package com.dajudge.kafkaproxy.networking.upstream;

import java.io.File;

public class ProxySslConfig {
    public static final ProxySslConfig DISABLED = new ProxySslConfig(false, null, null, null, null, null);

    private final boolean enabled;
    private final File trustStore;
    private final String trustStorePassword;
    private final File keyStore;
    private final String keyStorePassword;
    private final String keyPassword;

    public ProxySslConfig(
            final boolean enabled,
            final File trustStore,
            final String trustStorePassword,
            final File keyStore,
            final String keyStorePassword,
            final String keyPassword
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public File getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public File getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
