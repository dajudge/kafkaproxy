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

package com.dajudge.kafkaproxy.roundtrip.cluster;

import org.testcontainers.containers.GenericContainer;

final class StarterThread<T extends GenericContainer<T>> extends Thread {
    private final T container;
    private RuntimeException error;

    StarterThread(final T container) {
        this.container = container;
    }

    @Override
    public void run() {
        try {
            container.start();
        } catch (final RuntimeException e) {
            error = e;
        }
    }

    void waitForStartup() {
        try {
            join(300000);
            if (isAlive()) {
                throw new RuntimeException("Starter thread did not complete in time");
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    T getContainer() {
        if (error != null) {
            throw error;
        }
        return container;
    }
}
