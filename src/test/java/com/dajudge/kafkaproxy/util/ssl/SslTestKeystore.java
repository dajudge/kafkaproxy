package com.dajudge.kafkaproxy.util.ssl;

import java.io.File;

public class SslTestKeystore {
    private final File jks;
    private final String keystorePassword;
    private final File keystorePasswordFile;
    private final String keyPassword;
    private final File keyPasswordFile;

    public SslTestKeystore(
            final File jks,
            final String keystorePassword,
            final File keystorePasswordFile,
            final String keyPassword,
            final File keyPasswordFile
    ) {
        this.jks = jks;
        this.keystorePassword = keystorePassword;
        this.keystorePasswordFile = keystorePasswordFile;
        this.keyPassword = keyPassword;
        this.keyPasswordFile = keyPasswordFile;
    }

    public File getKeystorePasswordFile() {
        return keystorePasswordFile;
    }

    public File getKeyPasswordFile() {
        return keyPasswordFile;
    }

    public File getKeyStore() {
        return jks;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
