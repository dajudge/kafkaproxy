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

package com.dajudge.kafkaproxy.config.broker;

import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.config.ConfigSource;
import com.dajudge.kafkaproxy.config.Environment;

import static java.lang.Integer.parseUnsignedInt;

public class BrokerConfigSource implements ConfigSource<BrokerConfig> {

    @Override
    public Class<BrokerConfig> getConfigClass() {
        return BrokerConfig.class;
    }

    @Override
    public BrokerConfig parse(final Environment environment) {
        return new BrokerConfig(
                getBootstrapBrokers(environment),
                environment.requiredString("KAFKAPROXY_CLIENT_HOSTNAME"),
                environment.requiredInt("KAFKAPROXY_CLIENT_BASE_PORT")
        );
    }

    private BrokerMapping.Endpoint getBootstrapBrokers(final Environment environment) {
        final String bootstrapServer = environment.requiredString("KAFKAPROXY_KAFKA_BOOTSTRAP_SERVER");
        final String[] bootstrapServerParts = bootstrapServer.split(":");
        return new BrokerMapping.Endpoint(bootstrapServerParts[0], parseUnsignedInt(bootstrapServerParts[1]));
    }
}
