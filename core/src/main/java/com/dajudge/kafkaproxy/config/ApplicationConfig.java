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

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.config.broker.BrokerConfig;
import com.dajudge.kafkaproxy.config.broker.BrokerConfigSource;
import com.dajudge.kafkaproxy.config.kafkassl.KafkaSslConfigSource;
import com.dajudge.kafkaproxy.config.proxyssl.ProxySslConfigSource;
import com.dajudge.kafkaproxy.networking.downstream.KafkaSslConfig;
import com.dajudge.kafkaproxy.networking.upstream.ProxySslConfig;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class ApplicationConfig {
    private final Environment environment;
    private final Map<Class<?>, ConfigSource<?>> sources = unmodifiableMap(new HashMap<Class<?>, ConfigSource<?>>() {{
        put(BrokerConfig.class, new BrokerConfigSource());
        put(ProxySslConfig.class, new ProxySslConfigSource());
        put(KafkaSslConfig.class, new KafkaSslConfigSource());
    }});

    public ApplicationConfig(final Environment environment) {
        this.environment = environment;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final Class<T> configClass) {
        return (T) sources.get(configClass).parse(environment);
    }
}
