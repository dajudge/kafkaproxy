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

package com.dajudge.kafkaproxy.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Util {
    private Util() {
    }

    @SuppressFBWarnings(
            value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "False positive" // see https://sourceforge.net/p/findbugs/bugs/1169/
    )
    static String toString(final FileResource res) {
        try (final InputStream is = res.get()) {
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
