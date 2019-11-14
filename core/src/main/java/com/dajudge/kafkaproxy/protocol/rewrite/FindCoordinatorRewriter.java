package com.dajudge.kafkaproxy.protocol.rewrite;

import com.dajudge.kafkaproxy.brokermap.BrokerMap;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import org.apache.kafka.common.message.FindCoordinatorResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.FindCoordinatorResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class FindCoordinatorRewriter extends BaseReflectingRewriter<FindCoordinatorResponse> {
    private final Logger LOG = LoggerFactory.getLogger(FindCoordinatorRewriter.class);
    private final BrokerMap brokerMap;

    public FindCoordinatorRewriter(final BrokerMap brokerMap) {
        this.brokerMap = brokerMap;
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
        final BrokerMapping mapping = brokerMap.getByBrokerEndpoint(data.host(), data.port());
        if (mapping == null) {
            LOG.error("Unknown broker node seen in {}: {}:{}", ApiKeys.FIND_COORDINATOR, data.host(), data.port());
        } else {
            data.setHost(mapping.getProxy().getHost());
            data.setPort(mapping.getProxy().getPort());
            LOG.debug(
                    "Rewriting {}: {}:{} -> {}:{}",
                    ApiKeys.FIND_COORDINATOR,
                    data.host(),
                    data.port(),
                    mapping.getProxy().getHost(),
                    mapping.getProxy().getPort()
            );
        }
    }
}
