package com.dajudge.kafkaproxy.networking.upstream;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;

import java.util.function.Consumer;

public interface ForwardChannel extends Consumer<ByteBuf> {
    ChannelFuture close();
}
