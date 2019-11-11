package com.dajudge.kafkaproxy.protocol.rewrite;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.apache.kafka.common.requests.AbstractResponse.parseResponse;

public abstract class BaseReflectingRewriter<T extends AbstractResponse> implements ResponseRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(BaseReflectingRewriter.class);

    @SuppressWarnings("unchecked")
    @Override
    public final Optional<ByteBuf> rewrite(final RequestHeader requestHeader, final ResponseHeader responseHeader, final ByteBuffer responseBuffer) {
        if (!appliesTo(requestHeader)) {
            return Optional.empty();
        }
        final short apiVersion = requestHeader.apiVersion();
        final ApiKeys apiKey = requestHeader.apiKey();
        final Struct responseStruct = apiKey.parseResponse(apiVersion, responseBuffer);
        LOG.trace("Original: {}", responseStruct);
        final T response = (T) parseResponse(apiKey, responseStruct, apiVersion);
        try {
            rewrite(response);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to rewrite metadata response", e);
        }
        LOG.trace("Modified response: {}", response);
        final ByteBuf serialized = Unpooled.wrappedBuffer(response.serialize(apiVersion, responseHeader));
        LOG.trace("Serialized bytes: {}", serialized.readableBytes());
        final ByteBuf lengthBuffer = Unpooled.wrappedBuffer(new byte[4]).resetWriterIndex();
        lengthBuffer.writeInt(serialized.readableBytes());
        return Optional.of(Unpooled.wrappedBuffer(lengthBuffer, serialized));
    }

    protected abstract void rewrite(final T response) throws NoSuchFieldException, IllegalAccessException;

    @Override
    public boolean appliesTo(final RequestHeader requestHeader) {
        return requestHeader.apiKey() != ApiKeys.METADATA;
    }
}
