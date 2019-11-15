package com.dajudge.kafkaproxy.util.environment;

import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.config.FileResource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.nio.file.Files.readAllBytes;

public class TestEnvironment implements Environment {
    private final Map<String, String> env = new HashMap<>();
    private final Map<String, FileResource> files = new HashMap<>();

    public TestEnvironment withEnv(final String var, final String value) {
        env.put(var, value);
        return this;
    }

    public TestEnvironment withFile(final String name, final String resource) {
        files.put(name, new ClasspathFileResource(resource));
        return this;
    }

    public TestEnvironment withFile(final String name, final byte[] data) {
        files.put(name, new ByteArrayFileResource(data));
        return this;
    }

    public TestEnvironment withFile(final String name, final File file) {
        try {
            return withFile(name, readAllBytes(file.toPath()));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
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
    public FileResource requiredFile(final String variable) {
        return file(requiredString(variable));
    }

    @Override
    public FileResource requiredFile(final String variable, final String defaultValue) {
        return file(requiredString(variable, defaultValue));
    }

    @Override
    public Optional<FileResource> optionalFile(final String variable) {
        return optionalString(variable).map(this::file);
    }

    @Override
    public boolean requiredBoolean(final String variable, final boolean defaultValue) {
        return optionalString(variable).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private FileResource file(final String filename) {
        if (!files.containsKey(filename)) {
            throw new IllegalArgumentException("File does not exist: " + filename);
        }
        return files.get(filename);
    }
}
