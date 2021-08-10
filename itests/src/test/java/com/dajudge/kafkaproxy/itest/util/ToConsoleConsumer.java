/*
 * Copyright 2019-2020 The kafkaproxy developers (see CONTRIBUTORS)
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
package com.dajudge.kafkaproxy.itest.util;

import org.testcontainers.containers.output.OutputFrame;

import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;

public class ToConsoleConsumer implements Consumer<OutputFrame> {
    private final PrintStream stream;

    public ToConsoleConsumer(final PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public void accept(final OutputFrame outputFrame) {
        try {

            final byte[] bytes = outputFrame.getBytes();
            if (bytes != null) {
                stream.write(bytes);
                stream.flush();
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
