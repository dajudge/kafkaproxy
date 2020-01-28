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

import java.util.Optional;

public interface Environment {
    String requiredString(final String variable, final String defaultValue);

    String requiredString(String variable);

    Optional<String> optionalString(final String variable);

    Optional<FileResource> optionalFile(String variable);

    boolean requiredBoolean(String variable, boolean defaultValue);

    int requiredInt(String proxy_base_port);

    FileResource requiredFile(String filename);
}
