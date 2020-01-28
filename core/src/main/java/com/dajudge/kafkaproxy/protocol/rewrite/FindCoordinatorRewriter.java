/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.kafkaproxy.protocol.rewrite;

import com.dajudge.kafkaproxy.ProxyChannelManager;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping.Endpoint;
import org.apache.kafka.common.message.FindCoordinatorResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.FindCoordinatorResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class FindCoordinatorRewriter extends BaseReflectingRewriter<FindCoordinatorResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(FindCoordinatorRewriter.class);
    private final ProxyChannelManager proxyChannelManager;

    public FindCoordinatorRewriter(final ProxyChannelManager proxyChannelManager) {
        this.proxyChannelManager = proxyChannelManager;
    }

    @Override
    public boolean appliesTo(final RequestHeader requestHeader) {
        return requestHeader.apiKey() == ApiKeys.FIND_COORDINATOR;
    }


    @Override
    protected void rewrite(final FindCoordinatorResponse response) throws NoSuchFieldException, IllegalAccessException {
        final Field field = FindCoordinatorResponse.class.getDeclaredField("data");
        field.setAccessible(true);
        final FindCoordinatorResponseData data = (FindCoordinatorResponseData) field.get(response);
        if (data.host() == null || data.host().isEmpty()) {
            return;
        }
        final BrokerMapping mapping = proxyChannelManager.getByBrokerEndpoint(new Endpoint(data.host(), data.port()));
        LOG.debug(
                "Rewriting {}: {}:{} -> {}:{}",
                ApiKeys.FIND_COORDINATOR,
                data.host(),
                data.port(),
                mapping.getProxy().getHost(),
                mapping.getProxy().getPort()
        );
        data.setHost(mapping.getProxy().getHost());
        data.setPort(mapping.getProxy().getPort());
    }
}
