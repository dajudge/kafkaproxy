/*
 * Copyright 2019 Alex Stockinger
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

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.config.ConfigSource;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class BrokerConfigSource implements ConfigSource<BrokerConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerConfigSource.class);
    private static final String ENV_BROKERMAP_LOCATION = PREFIX + "BROKERMAP_LOCATION";
    private static final String DEFAULT_BROKERMAP_LOCATION = "/etc/kafkaproxy/brokermap.yml";

    @Override
    public Class<BrokerConfig> getConfigClass() {
        return BrokerConfig.class;
    }

    @Override
    public BrokerConfig parse(final Environment environment) {
        final BrokerMap brokerMap = getBrokerMap(environment);
        brokerMap.getAll().forEach(b -> LOG.debug("Found brokermap entry: {}", b));
        return new BrokerConfig(brokerMap);
    }

    private BrokerMap getBrokerMap(final Environment environment) {
        final FileResource brokerMapFile = environment.requiredFile(ENV_BROKERMAP_LOCATION, DEFAULT_BROKERMAP_LOCATION);
        try (final InputStream inputStream = brokerMapFile.open()) {
            return new BrokerMapParser(inputStream).getBrokerMap();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to read broker map", e);
        }
    }
}
