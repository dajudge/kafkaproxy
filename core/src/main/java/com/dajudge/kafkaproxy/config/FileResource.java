package com.dajudge.kafkaproxy.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public interface FileResource {
    InputStream open();

    static FileResource fromFile(final File file) {
        return () -> {
            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                throw new IllegalStateException("File not found.", e);
            }
        };
    }
}
