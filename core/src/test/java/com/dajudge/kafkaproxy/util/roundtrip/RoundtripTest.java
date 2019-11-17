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

package com.dajudge.kafkaproxy.util.roundtrip;

import com.dajudge.kafkaproxy.ProxyApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class RoundtripTest implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RoundtripTest.class);
    private final ProxyApplication proxyApp;
    private final RoundtripTester tester;
    private final RoundtripCounter roundtrip;
    private final Collection<AutoCloseable> resources;

    public RoundtripTest(
            final ProxyApplication proxyApp,
            final RoundtripTester tester,
            final RoundtripCounter roundtrip,
            final Collection<AutoCloseable> resources
    ) {
        this.proxyApp = proxyApp;
        this.tester = tester;
        this.roundtrip = roundtrip;
        this.resources = resources;
    }

    @Override
    public void close() {
        proxyApp.shutdown();
        resources.forEach(r -> {
            try {
                r.close();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to close resource", e);
            }
        });
    }

    public void run() {
        final long start = System.currentTimeMillis();
        tester.run(roundtrip);
        final long end = System.currentTimeMillis();
        assertTrue("Did not complete roundtrip", roundtrip.completed());
        LOG.info("Executed roundtrip in {}ms", end - start);
    }
}
