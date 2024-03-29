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

package com.dajudge.kafkaproxy;


import com.dajudge.kafkaproxy.config.RealEnvironment;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import static com.dajudge.kafkaproxy.KafkaProxyApplication.create;
import static com.dajudge.proxybase.certs.Filesystem.DEFAULT_FILESYSTEM;

@ApplicationScoped
public class Startup {
    private KafkaProxyApplication application;

    void onStart(@Observes StartupEvent ev) {
        application = create(new RealEnvironment(), System::currentTimeMillis, DEFAULT_FILESYSTEM);
    }

    void onStop(@Observes ShutdownEvent ev) {
        application.close();
    }
}
