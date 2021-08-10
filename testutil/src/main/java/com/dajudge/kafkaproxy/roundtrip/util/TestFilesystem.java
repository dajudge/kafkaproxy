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
