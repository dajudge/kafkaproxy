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

package com.dajudge.kafkaproxy.networking.trustmanager;

import org.apache.hc.client5.http.ssl.HttpsSupport;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpClientHostnameCheck implements HostnameCheck {
    public static final javax.net.ssl.HostnameVerifier VERIFIER = HttpsSupport.getDefaultHostnameVerifier();
    private final String hostname;

    public HttpClientHostnameCheck(final String hostname) {
        this.hostname = hostname;
    }

    @Override
    public void verify(final X509Certificate cert) throws CertificateException {
        if (!VERIFIER.verify(hostname, new DummySslSession(cert))) {
            throw new CertificateException("Certificate does not match hostname: " + hostname);
        }
    }

}
