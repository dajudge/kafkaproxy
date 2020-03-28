package com.dajudge.proxybase.ca;

import javax.net.ssl.SSLPeerUnverifiedException;

public interface CertificateAuthority {
    KeyStoreWrapper createClientCertificate(UpstreamCertificateSupplier certificateSupplier)
            throws SSLPeerUnverifiedException;
}
