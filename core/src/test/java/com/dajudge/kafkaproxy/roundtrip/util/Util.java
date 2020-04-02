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

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class Util {
    private Util() {
    }

    @NotNull
    public static String indent(final int spaces, final String string) {
        return indent(new String(new char[spaces]).replace("\0", " "), string);
    }

    @NotNull
    public static String indent(final String prefix, final String string) {
        return string.replaceAll("(?m)^", prefix);
    }

    @NotNull
    public static String randomIdentifier() {
        return "i" + UUID.randomUUID().toString().replace("-", "");
    }
}
