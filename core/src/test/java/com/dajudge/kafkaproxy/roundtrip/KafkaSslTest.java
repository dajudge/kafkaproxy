/*
 * Copyright 2019 Alex Stockinger
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

package com.dajudge.kafkaproxy.roundtrip;

import com.dajudge.kafkaproxy.util.roundtrip.RoundtripTest;

import static com.dajudge.kafkaproxy.util.roundtrip.RoundtripTestBuilder.roundtripTest;
import static java.util.Collections.singletonList;

public class KafkaSslTest extends BaseRoundtripTest {

    @Override
    protected RoundtripTest build() {
        return roundtripTest()
                .withPlaintextClient("localhost")
                .withSslKafka(singletonList("localhost"))
                .build();
    }
}
