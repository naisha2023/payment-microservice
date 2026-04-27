package org.example.paymentservice.config;

import javax.net.ssl.SSLContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class MtlsConfig {

    @Value("${mtls.key-store}")
    private Resource keyStore;

    @Value("${mtls.key-store-password}")
    private String keyStorePassword;

    @Value("${mtls.trust-store}")
    private Resource trustStore;

    @Value("${mtls.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public SSLContext sslContext() throws Exception {
        return SSLContexts.custom()
            .loadKeyMaterial(
                keyStore.getFile(),
                keyStorePassword.toCharArray(),
                keyStorePassword.toCharArray()
            )
            .loadTrustMaterial(
                trustStore.getFile(),
                trustStorePassword.toCharArray()
            )
            .build();
    }
}