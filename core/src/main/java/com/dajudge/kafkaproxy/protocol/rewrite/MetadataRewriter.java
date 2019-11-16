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

package com.dajudge.kafkaproxy.protocol.rewrite;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class MetadataRewriter extends BaseReflectingRewriter<MetadataResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(MetadataRewriter.class);
    private final BrokerMap brokerMap;

    public MetadataRewriter(final BrokerMap brokerMap) {
        this.brokerMap = brokerMap;
    }

    @Override
    protected void rewrite(final MetadataResponse response) throws NoSuchFieldException, IllegalAccessException {
        final Field field = MetadataResponse.class.getDeclaredField("data");
        field.setAccessible(true);
        final MetadataResponseData data = (MetadataResponseData) field.get(response);
        data.brokers().forEach(b -> {
            final BrokerMapping mapping = brokerMap.getByBrokerEndpoint(b.host(), b.port());
            if (mapping == null) {
                LOG.error("Unknown broker node seen in {}: {}:{}", ApiKeys.METADATA, b.host(), b.port());
            } else {
                LOG.debug(
                        "Rewriting {}: {}:{} -> {}:{}",
                        ApiKeys.METADATA,
                        b.host(),
                        b.port(),
                        mapping.getProxy().getHost(),
                        mapping.getProxy().getPort()
                );
                b.setHost(mapping.getProxy().getHost());
                b.setPort(mapping.getProxy().getPort());
            }
        });
    }

    @Override
    public boolean appliesTo(final RequestHeader requestHeader) {
        return requestHeader.apiKey() == ApiKeys.METADATA;
    }
}
