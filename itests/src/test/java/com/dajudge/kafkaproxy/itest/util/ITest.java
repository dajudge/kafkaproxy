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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

public class ITest extends GenericContainer<ITest> {
    private static final Logger LOG = LoggerFactory.getLogger(ITest.class);
    private static final AbstractImagePullPolicy NEVER = new AbstractImagePullPolicy() {
        @Override
        protected boolean shouldPullCached(
                final DockerImageName dockerImageName,
                final ImageData imageData
        ) {
            return false;
        }
    };

    public ITest(final String image) {
        super(image);
        this.withImagePullPolicy(NEVER)
                .withNetworkMode("host")
                .withLogConsumer(outputFrame -> LOG.info(outputFrame.getUtf8String()));
    }

    public Integer exec(final String... command) throws InterruptedException, IOException {
        final InspectContainerResponse containerInfo = getContainerInfo();
        final String containerId = containerInfo.getId();
        final DockerClient dockerClient = DockerClientFactory.instance().client();
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(command)
                .exec();
        final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        Throwable error = null;

        try {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, new ToConsoleConsumer(System.out));
            callback.addConsumer(OutputFrame.OutputType.STDERR, new ToConsoleConsumer(System.out));
            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        } catch (final Exception err) {
            error = err;
            throw err;
        } finally {
            if (error != null) {
                try {
                    callback.close();
                } catch (final Exception closeError) {
                    error.addSuppressed(closeError);
                }
            } else {
                callback.close();
            }
        }

        return dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCode();
    }
}
