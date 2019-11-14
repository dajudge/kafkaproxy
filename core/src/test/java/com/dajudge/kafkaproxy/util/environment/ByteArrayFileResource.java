package com.dajudge.kafkaproxy.util.environment;

import com.dajudge.kafkaproxy.config.FileResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayFileResource implements FileResource {
    private final byte[] data;

    public ByteArrayFileResource(final byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream open() {
        return new ByteArrayInputStream(data);
    }
}
