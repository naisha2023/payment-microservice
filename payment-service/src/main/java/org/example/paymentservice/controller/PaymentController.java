package org.example.paymentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.dtos.ApiResponse;
import org.example.paymentservice.dto.CreatePaymentRequest;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.service.PaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para operaciones de pagos
 */
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints para gestión de pagos")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Crear pago", description = "Crea un nuevo pago y reserva fondos en la wallet")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        log.info("Solicitud de creación de pago recibida");
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        PaymentResponse payment = paymentService.createPayment(request, jwt, authHeader);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pago creado exitosamente", payment));
    }

    @Operation(summary = "Confirmar pago", description = "Confirma un pago y deduce los fondos de la wallet")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        log.info("Solicitud de confirmación de pago: {}", paymentId);
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        PaymentResponse payment = paymentService.confirmPayment(paymentId, jwt, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Pago confirmado exitosamente", payment));
    }

    @Operation(summary = "Cancelar pago", description = "Cancela un pago y libera los fondos reservados")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancel(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        log.info("Solicitud de cancelación de pago: {}", paymentId);
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        PaymentResponse payment = paymentService.cancelPayment(paymentId, jwt, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Pago cancelado exitosamente", payment));
    }

    @Operation(summary = "Obtener pago por ID", description = "Devuelve la información de un pago específico")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Solicitud de obtención de pago: {}", paymentId);
        PaymentResponse payment = paymentService.getById(paymentId, jwt);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @Operation(summary = "Obtener mis pagos", description = "Devuelve todos los pagos del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> myPayments(@AuthenticationPrincipal Jwt jwt) {
        log.info("Solicitud de lista de pagos del usuario");
        List<PaymentResponse> payments = paymentService.getMyPayments(jwt);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
