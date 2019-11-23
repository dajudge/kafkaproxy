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

public class SslTestKeyStore {
    private final File jks;
    private final String keyStorePassword;
    private final File keyStorePasswordFile;
    private final String keyPassword;
    private final File keyPasswordFile;

    public SslTestKeyStore(
            final File jks,
            final String keyStorePassword,
            final File keyStorePasswordFile,
            final String keyPassword,
            final File keyPasswordFile
    ) {
        this.jks = jks;
        this.keyStorePassword = keyStorePassword;
        this.keyStorePasswordFile = keyStorePasswordFile;
        this.keyPassword = keyPassword;
        this.keyPasswordFile = keyPasswordFile;
    }

    public File getKeyStorePasswordFile() {
        return keyStorePasswordFile;
    }

    public File getKeyPasswordFile() {
        return keyPasswordFile;
    }

    public File getKeyStore() {
        return jks;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
