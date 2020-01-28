/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.kafkaproxy.roundtrip.comm;

import org.testcontainers.images.builder.Transferable;

import java.util.function.BiConsumer;

public class PlaintextServerSecurity implements ServerSecurity {
    @Override
    public String getClientProtocol() {
        return "PLAINTEXT";
    }

    @Override
    public String getTrustStoreLocation() {
        return null;
    }

    @Override
    public String getTrustStorePassword() {
        return null;
    }

    @Override
    public String getKeyStoreLocation() {
        return null;
    }

    @Override
    public String getKeyStorePassword() {
        return null;
    }

    @Override
    public String getKeyPassword() {
        return null;
    }

    @Override
    public String getClientAuth() {
        return "none";
    }

    @Override
    public byte[] getKeyStore() {
        return null;
    }

    @Override
    public byte[] getTrustStore() {
        return null;
    }

    @Override
    public void uploadKeyStores(final BiConsumer<Transferable, String> uploader) {
    }
}
