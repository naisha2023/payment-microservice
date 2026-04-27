package org.example.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
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

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r
                    .path("/api/auth/**")
                    .filters(f -> f
                        .stripPrefix(1)
                        .requestRateLimiter(c -> c
                            .setRateLimiter(redisRateLimiter())
                            .setKeyResolver(userKeyResolver())
                        )
                        .addRequestHeader("X-Gateway-Source", "api-gateway")
                    )
                    .uri("lb://auth-service")
                )
            .route("wallet-service", r -> r
                    .path("/api/wallets/**")
                    .filters(f -> f
                        .stripPrefix(1)
                        .requestRateLimiter(c -> c
                            .setRateLimiter(redisRateLimiter())
                            .setKeyResolver(userKeyResolver())
                        )
                    )
                    .uri("lb://wallet-service")
                )
                .route("payment-service", r -> r
                    .path("/api/payments/**")
                    .filters(f -> f
                        .stripPrefix(1)
                        .requestRateLimiter(c -> c
                            .setRateLimiter(redisRateLimiter())
                            .setKeyResolver(userKeyResolver())
                        )
                    )
                    .uri("lb://payment-service")
                )
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20); // 10 req/s, burst 20
    }

    @Bean
    public KeyResolver userKeyResolver() {
        // Limitar por X-User-Id (viene del JwtAuthFilter)
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getHeaders().getFirst("X-User-Id")
        ).defaultIfEmpty("anonymous");
    }
}
