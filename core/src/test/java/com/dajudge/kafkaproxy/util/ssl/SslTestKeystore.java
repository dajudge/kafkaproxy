/*
 * Copyright 2019 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
