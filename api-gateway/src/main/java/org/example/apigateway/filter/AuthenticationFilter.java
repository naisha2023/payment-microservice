package org.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtro de autenticación para el API Gateway
 * Valida la presencia de tokens JWT en las rutas protegidas
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/actuator/health");

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // Permitir acceso a endpoints públicos
            if (isPublicEndpoint(path)) {
                log.debug("Acceso permitido a endpoint público: {}", path);
                return chain.filter(exchange);
            }

            // Validar presencia de token
            if (!hasAuthorizationHeader(exchange)) {
                log.warn("Acceso denegado - Sin token de autorización: {}", path);
                return onError(exchange, "Token de autorización no encontrado", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (!isValidAuthorizationHeader(authHeader)) {
                log.warn("Acceso denegado - Token inválido: {}", path);
                return onError(exchange, "Token de autorización inválido", HttpStatus.UNAUTHORIZED);
            }

            log.debug("Token válido para: {}", path);
            return chain.filter(exchange);
        };
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean hasAuthorizationHeader(ServerWebExchange exchange) {
        String auth = exchange.getRequest()
        .getHeaders()
        .getFirst(HttpHeaders.AUTHORIZATION);

        return auth != null && auth.startsWith("Bearer ");
    }

    private boolean isValidAuthorizationHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
                "{\"timestamp\":\"%s\",\"success\":false,\"message\":\"%s\",\"error\":{\"code\":%d,\"type\":\"%s\"}}",
                java.time.Instant.now(),
                message,
                status.value(),
                status.getReasonPhrase());

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes())));
    }

    public static class Config {
        // Configuración del filtro si es necesaria
    }
}
