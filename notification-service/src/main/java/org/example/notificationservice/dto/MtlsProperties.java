package org.example.notificationservice.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mtls")
public record MtlsProperties(
    String keyStore,
    String keyStorePassword,
    String trustStore,
    String trustStorePassword
) {}