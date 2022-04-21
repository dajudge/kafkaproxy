/*
 * Copyright 2019-2021 The kafkaproxy developers (see CONTRIBUTORS)
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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class PortFinder implements AutoCloseable {
    private final List<ServerSocket> sockets = new ArrayList<>();

    @SuppressWarnings("PMD.CloseResource")
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
