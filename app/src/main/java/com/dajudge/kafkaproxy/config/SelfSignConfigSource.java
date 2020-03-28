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

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.ca.selfsign.SelfSignConfig;

public class SelfSignConfigSource implements ConfigSource<SelfSignConfig> {
    private static final String PREFIX = ConfigSource.PREFIX + "SELFSIGN_";
    private static final String PROP_SELFSIGN_ISSUERDN = PREFIX + "ISSUERDN";
    private static final String PROP_SELFSIGN_KEYSTORE_LOCATION = PREFIX + "KEYSTORE_LOCATION";
    private static final String PROP_SELFSIGN_KEYSTORE_PASSWORD = PREFIX + "KEYSTORE_PASSWORD";
    private static final String PROP_SELFSIGN_KEY_ALIAS = PREFIX + "KEY_ALIAS";
    private static final String PROP_SELFSIGN_KEY_PASSWORD = PREFIX + "KEY_PASSWORD";
    private static final String PROP_SELFSIGN_SIGNATURE_ALGORITHM = PREFIX + "SIGNATURE_ALGORITHM";
    private static final String DEAFULT_ALGORITHM = "SHA256withRSA";

    @Override
    public Class<SelfSignConfig> getConfigClass() {
        return SelfSignConfig.class;
    }

    @Override
    public SelfSignConfig parse(final Environment environment) {
        return new SelfSignConfig(
                environment.requiredString(PROP_SELFSIGN_ISSUERDN),
                environment.requiredFile(PROP_SELFSIGN_KEYSTORE_LOCATION),
                environment.optionalString(PROP_SELFSIGN_KEYSTORE_PASSWORD).orElse(null),
                environment.requiredString(PROP_SELFSIGN_KEY_ALIAS),
                environment.optionalString(PROP_SELFSIGN_KEY_PASSWORD).orElse(null),
                environment.requiredString(PROP_SELFSIGN_SIGNATURE_ALGORITHM, DEAFULT_ALGORITHM)
        );
    }
}
