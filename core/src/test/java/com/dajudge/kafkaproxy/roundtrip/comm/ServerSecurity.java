/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

public interface ServerSecurity {
    String getClientProtocol();

    String getTrustStoreLocation();

    char[] getTrustStorePassword();

    String getTrustStoreType();

    String getKeyStoreLocation();

    char[] getKeyStorePassword();

    String getKeyStoreType();

    char[] getKeyPassword();

    String getClientAuth();

    byte[] getKeyStore();

    byte[] getTrustStore();

    void uploadKeyStores(BiConsumer<Transferable, String> uploader);
}
