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

package com.dajudge.kafkaproxy;

import com.dajudge.kafkaproxy.config.ApplicationConfig;
import com.dajudge.kafkaproxy.config.Environment;
import com.dajudge.kafkaproxy.networking.upstream.ProxyChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public class ProxyApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApplication.class);
    private final Environment environment;
    private Runnable shutdownRunnable;

    private ProxyApplication(final Environment environment) {
        this.environment = environment;
    }

    public static ProxyApplication create(final Environment environment) {
        return new ProxyApplication(environment);
    }

    public void shutdown() {
        if (shutdownRunnable == null) {
            throw new IllegalStateException("must invoke start() first");
        }
        shutdownRunnable.run();
    }

    public ProxyApplication start() {
        final ApplicationConfig appConfig = new ApplicationConfig(environment);
        final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final ProxyChannelManager.ProxyChannelFactory channelFactory = new StaticProxyChannelFactory(
                appConfig,
                downstreamWorkerGroup,
                serverWorkerGroup,
                upstreamWorkerGroup
        );
        final ProxyChannelManager proxyChannelManager = new ProxyChannelManager(channelFactory);
        channelFactory.bootstrap(proxyChannelManager);
        shutdownRunnable = () -> {
            proxyChannelManager.proxies().stream()
                    .map(ProxyChannel::close)
                    .collect(toList())
                    .forEach(future -> {
                        try {
                            future.sync();
                        } catch (final Exception e) {
                            LOG.error("Failed to sync with proxy channel", e);
                        }
                    });
            serverWorkerGroup.shutdownGracefully();
            upstreamWorkerGroup.shutdownGracefully();
            downstreamWorkerGroup.shutdownGracefully();
        };
        return this;
    }

}
