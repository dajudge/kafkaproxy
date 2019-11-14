package com.dajudge.kafkaproxy.config;

import java.io.File;
import java.util.Optional;

import static com.dajudge.kafkaproxy.config.FileResource.fromFile;
import static java.lang.System.getenv;

public class RealEnvironment implements Environment {
    @Override
    public String requiredString(final String variable, final String defaultValue) {
        final String value = getenv(variable);
        return value == null ? defaultValue : value;
    }

    @Override
    public String requiredString(final String variable) {
        return Optional.ofNullable(requiredString(variable, null))
                .orElseThrow(() -> new IllegalArgumentException("No such environment variable: " + variable));
    }

    @Override
    public Optional<String> optionalString(final String variable) {
        return Optional.ofNullable(requiredString(variable, null));
    }

    @Override
    public FileResource requiredFile(final String variable) {
        return file(requiredString(variable, variable));
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
        final File file = new File(filename);
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Filesystem entry " + file.getAbsolutePath() + " is not a file");
        }
        return fromFile(file);
    }
}
