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

package com.dajudge.kafkaproxy.ca;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.KafkaBrokerConfigSource;
import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.ca.ClientCertCertificateAuthority;
import com.dajudge.proxybase.ca.NullCertificateAuthority;

import static com.dajudge.kafkaproxy.ca.CertificateAuthorityFactoryRegistry.createCertificateFactory;

public interface CertificateAuthorityFactory {
    String getName();

    CertificateAuthority createFactory(ApplicationConfig config);

    static CertificateAuthority createCertificateAuthority(final ApplicationConfig appConfig) {
        final KafkaBrokerConfigSource.KafkaBrokerConfig sslConfig = appConfig.get(KafkaBrokerConfigSource.KafkaBrokerConfig.class);
        switch (sslConfig.getClientCertificateStrategy()) {
            case KEYSTORE:
                return new ClientCertCertificateAuthority(sslConfig.getClientCertificateConfig());
            case CA:
                return createCertificateFactory(sslConfig.getCertificateFactory(), appConfig);
            case NONE:
                return NullCertificateAuthority.INSTANCE;
            default:
                throw new IllegalArgumentException("Unhandled client certificate strategy: "
                        + sslConfig.getClientCertificateStrategy());
        }
    }
}
