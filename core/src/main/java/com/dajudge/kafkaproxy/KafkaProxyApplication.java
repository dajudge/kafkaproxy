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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.BrokerConfigSource;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.KafkaBrokerConfigSource.KafkaBrokerConfig;
import com.dajudge.proxybase.ProxyApplication;
import com.dajudge.proxybase.ProxyChannel;
import com.dajudge.proxybase.ProxyChannelFactory;
import com.dajudge.proxybase.config.UpstreamConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.dajudge.kafkaproxy.ca.CertificateAuthorityFactory.createCertificateAuthority;

public class KafkaProxyApplication extends ProxyApplication {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProxyApplication.class);

    private final BrokerMapper brokerMappingStrategy;

    private KafkaProxyApplication(final ApplicationConfig appConfig) {
        super(
                appConfig.get(UpstreamConfig.class),
                appConfig.get(KafkaBrokerConfig.class).getDownstreamConfig(),
                createCertificateAuthority(appConfig)
        );
        brokerMappingStrategy = new BrokerMapper(appConfig.get(BrokerConfigSource.BrokerConfig.class));
    }

    public static ProxyApplication create(final Environment environment) {
        return new KafkaProxyApplication(new ApplicationConfig(environment));
    }

    @Override
    protected Collection<ProxyChannel> initializeProxyChannels(final ProxyChannelFactory proxyChannelFactory) {
        final KafkaProxyChannelFactory kafkaProxyChannelFactory = new KafkaProxyChannelFactory(
                brokerMappingStrategy,
                proxyChannelFactory
        );
        final KafkaProxyChannelManager proxyChannelManager = new KafkaProxyChannelManager(kafkaProxyChannelFactory);
        final BrokerMapping boostrapMapping = kafkaProxyChannelFactory.bootstrap(proxyChannelManager);
        LOG.info("Bootstrap broker mapping: {}", boostrapMapping);
        return proxyChannelManager.proxies();
    }
}
