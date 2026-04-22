package org.example.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * Configuración del API Gateway
 * Define filtros globales y configuraciones de enrutamiento
 */
@Slf4j
@Configuration
public class GatewayConfig {

    /**
     * Filtro global para logging de requests
     */
    @Bean
    @Order(1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            log.info("Incoming request: {} {}", method, path);

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        int statusCode = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value()
                                : 0;
                        log.info("Response: {} {} - Status: {}", method, path, statusCode);
                    }));
        };
    }

    /**
     * Filtro global para agregar headers de correlación
     */
    @Bean
    @Order(2)
    public GlobalFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders()
                    .getFirst("X-Correlation-ID");

            if (correlationId == null) {
                correlationId = java.util.UUID.randomUUID().toString();
            }

            final String finalCorrelationId = correlationId;

            return chain.filter(exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-Correlation-ID", finalCorrelationId)
                            .build())
                    .build())
                    .then(Mono.fromRunnable(() -> exchange.getResponse().getHeaders()
                            .add("X-Correlation-ID", finalCorrelationId)));
        };
    }
}
