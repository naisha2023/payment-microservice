package org.example.paymentservice.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlation_id");

            if (correlationId != null && !correlationId.isBlank()) {
                template.header("X-Correlation-ID", correlationId);
                template.header("X-Request-ID", correlationId);
            }
        };
    }
}