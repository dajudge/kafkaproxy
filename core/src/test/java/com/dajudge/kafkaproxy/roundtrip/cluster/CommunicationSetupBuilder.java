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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import com.dajudge.kafkaproxy.roundtrip.comm.CommunicationSetup;
import com.dajudge.kafkaproxy.roundtrip.comm.PlaintextCommunicationSetup;
import com.dajudge.kafkaproxy.roundtrip.comm.SslCommunicationSetup;
import org.jetbrains.annotations.NotNull;

public class CommunicationSetupBuilder {
    private CommunicationSetup comm;

    public CommunicationSetupBuilder() {
    }

    public CommunicationSetupBuilder withPlaintext() {
        comm = new PlaintextCommunicationSetup();
        return this;
    }

    public CommunicationSetupBuilder withSsl() {
        return withSsl(false);
    }

    public CommunicationSetupBuilder withMutualTls() {
        return withSsl(true);
    }

    @NotNull
    private CommunicationSetupBuilder withSsl(final boolean requireClientAuth) {
        comm = new SslCommunicationSetup(
                "CN=clientCA",
                "CN=brokerCA",
                requireClientAuth
        );
        return this;
    }

    public CommunicationSetup build() {
        return comm;
    }
}
