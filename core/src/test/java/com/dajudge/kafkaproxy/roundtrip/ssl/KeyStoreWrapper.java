/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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

package com.dajudge.kafkaproxy.roundtrip.ssl;

public class KeyStoreWrapper {
    private final byte[] keyStore;
    private final String keyStorePassword;
    private final String keyPassword;

    public KeyStoreWrapper(final byte[] keyStore, final String keyStorePassword, final String keyPassword) {
        this.keyStore = keyStore.clone();
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
    }

    public byte[] getBytes() {
        return keyStore.clone();
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
