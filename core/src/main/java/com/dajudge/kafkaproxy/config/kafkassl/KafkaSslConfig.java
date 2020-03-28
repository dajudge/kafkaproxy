package com.dajudge.kafkaproxy.config.kafkassl;

import com.dajudge.kafkaproxy.ca.ClientCertificateStrategy;
import com.dajudge.proxybase.ca.NullClientCertificateAuthorityFactory;
import com.dajudge.proxybase.config.DownstreamSslConfig;

public class KafkaSslConfig {
    private final DownstreamSslConfig downstreamSslConfig;
    private final String certificateFactory;
    private final ClientCertificateStrategy clientCertificateStrategy;
    public static final KafkaSslConfig DISABLED = new KafkaSslConfig(
            DownstreamSslConfig.DISABLED,
            NullClientCertificateAuthorityFactory.NAME,
            ClientCertificateStrategy.NONE
    );

    KafkaSslConfig(
            final DownstreamSslConfig downstreamSslConfig,
            final String certificateFactory,
            final ClientCertificateStrategy clientCertificateStrategy
    ) {
        this.downstreamSslConfig = downstreamSslConfig;
        this.certificateFactory = certificateFactory;
        this.clientCertificateStrategy = clientCertificateStrategy;
    }

    public DownstreamSslConfig getDownstreamSslConfig() {
        return downstreamSslConfig;
    }

    public String getCertificateFactory() {
        return certificateFactory;
    }

    public ClientCertificateStrategy getClientCertificateStrategy() {
        return clientCertificateStrategy;
    }
}
