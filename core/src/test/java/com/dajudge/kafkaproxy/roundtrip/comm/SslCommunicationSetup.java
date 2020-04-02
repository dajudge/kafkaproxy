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

package com.dajudge.kafkaproxy.roundtrip.comm;

import com.dajudge.kafkaproxy.roundtrip.ssl.CertAuthority;

import java.util.Optional;

public class SslCommunicationSetup implements CommunicationSetup {
    private final CertAuthority brokerAuthority;
    private final CertAuthority clientAuthority;
    private final boolean requireClientAuth;

    public SslCommunicationSetup(
            final String clientCaDn,
            final String brokerCaDn,
            final boolean requireClientAuth
    ) {
        this.brokerAuthority = new CertAuthority(brokerCaDn);
        this.clientAuthority = new CertAuthority(clientCaDn);
        this.requireClientAuth = requireClientAuth;
    }

    @Override
    public ServerSecurity getServerSecurity(final String dn) {
        return new SslServerSecurity(
                brokerAuthority.createSignedKeyPair(dn),
                clientAuthority.getTrustStore(),
                requireClientAuth
        );
    }


    @Override
    public ClientSecurity getClientSecurity() {
        return new SslClientSecurity(
                brokerAuthority.getTrustStore(),
                requireClientAuth ? Optional.of(clientAuthority::createSignedKeyPair) : Optional.empty(),
                requireClientAuth ? "KEYSTORE" : "NONE"
        );
    }

    @Override
    public String toString() {
        return "SslCommunicationSetup{" +
                "requireClientAuth=" + requireClientAuth +
                '}';
    }
}
