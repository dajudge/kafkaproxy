package com.dajudge.proxybase.ca;

public interface ConfigProvider {
    <T> T getConfig(Class<T> configClass);
}
