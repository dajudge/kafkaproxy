package com.dajudge.kafkaproxy.protocol;

import com.dajudge.kafkaproxy.brokermap.BrokerMapper;
import com.dajudge.kafkaproxy.brokermap.BrokerMapping;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.apache.kafka.common.requests.AbstractResponse.parseResponse;

public class MetadataRewriter implements ResponseRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(MetadataRewriter.class);
    private final BrokerMapper brokerMapper;

    public MetadataRewriter(final BrokerMapper brokerMapper) {
        this.brokerMapper = brokerMapper;
    }

    @Override
    public Optional<ByteBuf> rewrite(final RequestHeader requestHeader, final ResponseHeader responseHeader, final ByteBuffer responseBuffer) {
        final ApiKeys apiKey = requestHeader.apiKey();
        if (apiKey != ApiKeys.METADATA) {
            return Optional.empty();
        }
        final short apiVersion = requestHeader.apiVersion();
        final Struct responseStruct = apiKey.parseResponse(apiVersion, responseBuffer);
        LOG.trace("Original: {}", responseStruct);
        final MetadataResponse metadataResponse = (MetadataResponse) parseResponse(apiKey, responseStruct, apiVersion);
        LOG.trace("Response: {}", metadataResponse);
        try {
            final Field field = metadataResponse.getClass().getDeclaredField("data");
            field.setAccessible(true);
            final MetadataResponseData data = (MetadataResponseData) field.get(metadataResponse);
            data.brokers().forEach(b -> {
                final BrokerMapping mapping = brokerMapper.map(b.host(), b.port());
                if (mapping == null) {
                    LOG.error("Unknown broker node seen in metadata: {}:{}", b.host(), b.port());
                } else {
                    LOG.debug(
                            "Rewriting broker: {}:{} -> {}:{}",
                            b.host(),
                            b.port(),
                            mapping.getHost(),
                            mapping.getPort()
                    );
                    b.setPort(mapping.getPort());
                    b.setHost(mapping.getHost());
                }
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to rewrite metadata response", e);
        }
        LOG.trace("Modified response: {}", metadataResponse);
        final ByteBuf serialized = Unpooled.wrappedBuffer(metadataResponse.serialize(apiVersion, responseHeader));
        LOG.trace("Serialized bytes: {}", serialized.readableBytes());
        final ByteBuf lengthBuffer = Unpooled.wrappedBuffer(new byte[4]).resetWriterIndex();
        lengthBuffer.writeInt(serialized.readableBytes());
        return Optional.of(Unpooled.wrappedBuffer(lengthBuffer, serialized));
    }
}
