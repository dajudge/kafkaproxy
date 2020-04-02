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

package com.dajudge.proxybase;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

class HostCheckingTrustManager implements X509TrustManager {
    private final Collection<X509TrustManager> nextManagers;
    private final HostnameCheck hostnameCheck;

    HostCheckingTrustManager(
            final Collection<X509TrustManager> nextManagers,
            final HostnameCheck hostnameCheck
    ) {
        this.nextManagers = nextManagers;
        this.hostnameCheck = hostnameCheck;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        throw new CertificateException("Cannot check client certificate");
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        for (final X509TrustManager nextManager : nextManagers) {
            nextManager.checkServerTrusted(chain, authType);
        }
        hostnameCheck.verify(chain[0]);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
