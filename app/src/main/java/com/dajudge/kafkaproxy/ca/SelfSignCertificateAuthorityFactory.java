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

package com.dajudge.kafkaproxy.ca;

import com.dajudge.kafkaproxy.ca.selfsign.SelfSignCertificateAuthority;
import com.dajudge.kafkaproxy.ca.selfsign.SelfSignConfig;
import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.proxybase.ca.CertificateAuthority;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SelfSignCertificateAuthorityFactory implements CertificateAuthorityFactory {
    @Override
    public String getName() {
        return "selfsign";
    }

    @Override
    public CertificateAuthority createFactory(
            final ApplicationConfig appConfig
    ) {
        final SelfSignConfig config = appConfig.get(SelfSignConfig.class);
        try {
            return new SelfSignCertificateAuthority(config);
        } catch (final KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to create self-signing certificate authority", e);
        }
    }
}
