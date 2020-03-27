package com.dajudge.kafkaproxy.networking;

import com.dajudge.kafkaproxy.networking.upstream.ForwardChannel;

import java.util.function.Function;

public interface FilterFactory<T> extends Function<ForwardChannel<T>, ForwardChannel<T>> {
}
