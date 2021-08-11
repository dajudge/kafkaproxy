/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

public class ApplicationConfig {
    private final Environment environment;
    private final Map<Class<?>, ConfigSource<?>> sources = unmodifiableMap(new HashMap<Class<?>, ConfigSource<?>>() {{
        stream(load(ConfigSource.class).spliterator(), false)
                .forEach(source -> put(source.getConfigClass(), source));
    }});

    public ApplicationConfig(final Environment environment) {
        this.environment = environment;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> optional(final Class<T> configClass) {
        return (Optional<T>) sources.get(configClass).parse(environment);
    }

    @SuppressWarnings("unchecked")
    public <T> T require(final Class<T> configClass) {
        return ((Optional<T>) sources.get(configClass).parse(environment))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find config of type " + configClass.getName()
                ));
    }
}
