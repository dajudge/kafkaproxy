package com.dajudge.kafkaproxy.util.ssl;

import java.io.File;

public class SslTestKeystore {
    private final File jks;
    private final File keystorePassword;
    private final File keyPassword;

    public SslTestKeystore(final File jks, final File keystorePassword, final File keyPassword) {
        this.jks = jks;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }

    public String getPath() {
        return jks.getAbsolutePath();
    }

    public File getKeystorePassword() {
        return keystorePassword;
    }

    public File getKeyPassword() {
        return keyPassword;
    }
}
