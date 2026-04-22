package org.example.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("Error en API Gateway: {}", ex.getMessage(), ex);

        HttpStatusCode status = determineHttpStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = buildErrorResponse(ex, status);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error al serializar respuesta de error", e);
            return exchange.getResponse().setComplete();
        }
    }

    private HttpStatusCode determineHttpStatus(Throwable ex) {
    if (ex instanceof ResponseStatusException) {
        ResponseStatusException responseStatusException = (ResponseStatusException) ex;
        return responseStatusException.getStatusCode();
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
}

   private Map<String, Object> buildErrorResponse(Throwable ex, HttpStatusCode status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("success", false);
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "Error en el gateway");

        Map<String, Object> error = new HashMap<>();
        error.put("code", status.value());

        String type;
        if (status instanceof HttpStatus) {
            HttpStatus httpStatus = (HttpStatus) status;
            type = httpStatus.getReasonPhrase();
        } else {
            type = "HTTP " + status.value();
        }

        error.put("type", type);
        error.put("details", ex.getClass().getSimpleName());

        response.put("error", error);
        return response;
    }
}