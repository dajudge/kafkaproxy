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

package com.dajudge.kafkaproxy.config;

import com.dajudge.kafkaproxy.roundtrip.util.TestEnvironment;
import org.junit.Test;

abstract class BaseOptionalConfigTest<C> extends BaseConfigTest<C> {
    @Test
    public void returns_disabled() {
        assertDisabled(disable(new TestEnvironment()));
    }

    @Test
    public void default_is_disabled() {
        assertDisabled(new TestEnvironment());
    }

    abstract TestEnvironment disable(TestEnvironment testEnvironment);

    abstract void assertDisabled(Environment env);
}
