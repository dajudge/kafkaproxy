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

package com.dajudge.kafkaproxy.roundtrip.util;

import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.FileResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.dajudge.kafkaproxy.config.FileResource.fromFile;
import static java.lang.Integer.parseInt;

public class TestEnvironment implements Environment {
    private final Map<String, String> env = new HashMap<>();
    private final Map<String, FileResource> files = new HashMap<>();

    public TestEnvironment withEnv(final String var, final String value) {
        env.put(var, value);
        return this;
    }

    @Override
    public String requiredString(final String variable, final String defaultValue) {
        return optionalString(variable).orElse(defaultValue);
    }

    @Override
    public String requiredString(final String variable) {
        final String value = requiredString(variable, null);
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException("Environment variable not set: " + variable));
    }

    @Override
    public Optional<String> optionalString(final String variable) {
        return Optional.ofNullable(env.get(variable));
    }

    @Override
    public Optional<FileResource> optionalFile(final String variable) {
        return optionalString(variable).map(this::file);
    }

    @Override
    public boolean requiredBoolean(final String variable, final boolean defaultValue) {
        return optionalString(variable).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    @Override
    public int requiredInt(final String variable) {
        return parseInt(requiredString(variable));
    }

    @Override
    public FileResource requiredFile(final String filename) {
        if (!files.containsKey(filename)) {
            throw new RuntimeException("No such file resource: " + filename);
        }
        return files.get(filename);
    }

    private FileResource file(final String filename) {
        if (!files.containsKey(filename)) {
            throw new IllegalArgumentException("File does not exist: " + filename);
        }
        return files.get(filename);
    }

    public void dump(final Consumer<String> dumper) {
        env.forEach((k, v) -> dumper.accept(k + "=" + v));
        files.forEach((k, v) -> dumper.accept(k + " - file"));
    }

    public TestEnvironment withFile(final String path) {
        if (path != null) {
            files.put(path, fromFile(new File(path)));
        }
        return this;
    }

    public TestEnvironment withFile(final String path, final byte[] data) {
        if (path != null) {
            files.put(path, () -> new ByteArrayInputStream(data));
        }
        return this;
    }
}
