package com.dajudge.kafkaproxy.protocol.rewrite;

import io.netty.buffer.ByteBuf;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.requests.ResponseHeader;

import java.nio.ByteBuffer;
import java.util.Optional;

public interface ResponseRewriter {
    boolean appliesTo(RequestHeader requestHeader);

    Optional<ByteBuf> rewrite(RequestHeader requestHeader, ResponseHeader responseHeader, ByteBuffer responseBuffer);
}
