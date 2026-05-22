package org.example.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtDebugFilter implements GlobalFilter, Ordered {

    private static final Logger log =
        LoggerFactory.getLogger(JwtDebugFilter.class);

    @Override
    public Mono<Void> filter(
        ServerWebExchange exchange,
        GatewayFilterChain chain
    ) {

        String auth =
            exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

        log.info("AUTH HEADER => {}", auth);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}