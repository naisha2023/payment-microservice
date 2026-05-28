package org.example.paymentservice.exception;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentNotFound(PaymentNotFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "PAYMENT_NOT_FOUND");
    }

    @ExceptionHandler(PaymentNotAuthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentNotAuthorized(PaymentNotAuthorizedException ex) {
        log.warn("Payment access forbidden: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "PAYMENT_NOT_AUTHORIZED");
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentState(InvalidPaymentStateException ex) {
        log.warn("Invalid payment state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_PAYMENT_STATE");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();

            errors.put(fieldName, error.getDefaultMessage());
        });

        log.warn("Validation errors: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation errors")
                .data(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({
            FeignException.class,
            RetryableException.class,
            TimeoutException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleUpstreamUnavailable(Exception ex) {
        log.warn("Upstream service unavailable: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Upstream service is temporarily unavailable",
                "UPSTREAM_SERVICE_UNAVAILABLE"
        );
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker open: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service temporarily unavailable",
                "CIRCUIT_BREAKER_OPEN"
        );
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests",
                "RATE_LIMIT_EXCEEDED"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                ex.getClass().getSimpleName()
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String message,
            String type
    ) {
        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code(status.value())
                .type(type)
                .details(message)
                .build();

        ApiResponse<Void> response = ApiResponse.error(message, errorDetails);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(WalletServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletUnavailable(WalletServiceUnavailableException ex) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                "WALLET_SERVICE_UNAVAILABLE"
        );
    }
}