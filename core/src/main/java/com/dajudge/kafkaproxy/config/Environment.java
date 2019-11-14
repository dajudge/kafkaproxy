package com.dajudge.kafkaproxy.config;

import java.util.Optional;

public interface Environment {
    String requiredString(final String variable, final String defaultValue);

    String requiredString(String variable);

    Optional<String> optionalString(final String variable);

    FileResource requiredFile(final String variable);

    FileResource requiredFile(final String variable, final String defaultValue);

    Optional<FileResource> optionalFile(String variable);

    boolean requiredBoolean(String variable, boolean defaultValue);
}
