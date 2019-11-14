package com.dajudge.kafkaproxy.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Util {
    private Util() {
    }

    static String toString(final FileResource res) {
        try (final InputStream is = res.open()) {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            int read;
            final byte[] buffer = new byte[1024];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            return new String(os.toByteArray(), UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
