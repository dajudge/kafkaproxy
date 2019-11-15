package com.dajudge.kafkaproxy.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class PortFinder implements AutoCloseable {
    private final List<ServerSocket> sockets = new ArrayList<>();

    public int nextPort() {
        try {
            final ServerSocket s = new ServerSocket(0);
            sockets.add(s);
            return s.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open server socket", e);
        }
    }

    @Override
    public void close() {
        sockets.forEach(socket -> {
            try {
                socket.close();
            } catch (final IOException e) {
                throw new RuntimeException("Failed to close server socket", e);
            }
        });
    }
}
