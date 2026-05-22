// src/main/java/org/example/apigateway/config/HttpClientConfig.java
package org.example.apigateway.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class HttpClientConfig {

    @Value("${server.ssl.trust-store}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> {
            try {
                String path = trustStorePath.replace("file:", "");
                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                trustStore.load(new FileInputStream(path), trustStorePassword.toCharArray());

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
                );
                tmf.init(trustStore);

                SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(tmf)
                    .build();

                return httpClient.secure(spec -> spec
                    .sslContext(sslContext)
                    .handlerConfigurator(handler ->
                        handler.engine().setSSLParameters(
                            new javax.net.ssl.SSLParameters() {{
                                setEndpointIdentificationAlgorithm(""); // ← deshabilita hostname check
                            }}
                        )
                    )
                );
            } catch (Exception e) {
                throw new RuntimeException("Error configurando SSL del gateway", e);
            }
        };
    }
}