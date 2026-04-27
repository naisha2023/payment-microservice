package org.example.notificationservice.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.example.notificationservice.dto.MtlsProperties;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;

@Configuration
@RequiredArgsConstructor
public class MtlsConfig {

    private final MtlsProperties props;
    private final ResourceLoader resourceLoader;
   
    @Bean
    public SSLContext sslContext() throws Exception { 
        Resource keyStore = resourceLoader.getResource(props.keyStore());
        Resource trustStore = resourceLoader.getResource(props.trustStore());
        return SSLContexts.custom()
            .loadKeyMaterial(
                keyStore.getFile(),
                props.keyStorePassword().toCharArray(),
                props.keyStorePassword().toCharArray()
            )
            .loadTrustMaterial(
                trustStore.getFile(),
                props.trustStorePassword().toCharArray()
            )
            .build();
    }

    @Bean
    public RestTemplate restTemplate(SSLContext sslContext) throws Exception {
        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        return new RestTemplate(
            new HttpComponentsClientHttpRequestFactory(httpClient)
        );
    }
}