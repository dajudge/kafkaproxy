package com.dajudge.kafkaproxy.util.environment;

import com.dajudge.kafkaproxy.config.FileResource;

import java.io.InputStream;

public class ClasspathFileResource implements FileResource {
    private final String resourceName;

    public ClasspathFileResource(final String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public InputStream open() {
        return getClass().getClassLoader().getResourceAsStream(resourceName);
    }
}
