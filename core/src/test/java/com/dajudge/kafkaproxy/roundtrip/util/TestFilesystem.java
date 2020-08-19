package com.dajudge.kafkaproxy.roundtrip.util;

import com.dajudge.proxybase.certs.Filesystem;

import java.util.HashMap;
import java.util.Map;

public class TestFilesystem implements Filesystem {
    private Map<String, byte[]> files = new HashMap<>();

    public TestFilesystem withFile(final String path, final byte[] data) {
        if (path != null) {
            files.put(path, data);
        }
        return this;
    }

    @Override
    public byte[] readFile(final String path) {
        return files.get(path);
    }
}
