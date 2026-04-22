package org.example.walletservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.walletservice.dto.*;
import org.example.walletservice.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para operaciones de wallet
 */
@Slf4j
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Endpoints para gestión de wallets y operaciones financieras")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Obtener wallet por ID de usuario", description = "Devuelve la información de la wallet de un usuario específico")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Solicitud de wallet para userId: {}", userId);
        UUID authenticatedUserId = extractUserId(jwt);
        WalletResponse wallet = walletService.getWalletForUser(userId, authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @Operation(summary = "Obtener balance de wallet", description = "Devuelve el balance actual, reservado y disponible de una wallet")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{userId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable UUID userId) {
        log.info("Solicitud de balance para userId: {}", userId);
        BalanceResponse balance = walletService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @Operation(summary = "Reservar fondos", description = "Reserva fondos en la wallet para un pago pendiente")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/reserve")
    public ResponseEntity<ApiResponse<WalletResponse>> reserve(
            @PathVariable UUID userId,
            @Valid @RequestBody ReserveFundsRequest request) {
        log.info("Solicitud de reserva de fondos para userId: {}, monto: {}", userId, request.amount());
        WalletResponse wallet = walletService.reserve(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Fondos reservados exitosamente", wallet));
    }

    @Operation(summary = "Liberar fondos reservados", description = "Libera fondos previamente reservados")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/release")
    public ResponseEntity<ApiResponse<WalletResponse>> release(
            @PathVariable UUID userId,
            @Valid @RequestBody ReleaseFundsRequest request) {
        log.info("Solicitud de liberación de fondos para userId: {}, monto: {}", userId, request.amount());
        WalletResponse wallet = walletService.release(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Fondos liberados exitosamente", wallet));
    }

    @Operation(summary = "Confirmar débito", description = "Confirma un débito y deduce los fondos reservados del balance")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/confirm-debit")
    public ResponseEntity<ApiResponse<WalletResponse>> confirmDebit(
            @PathVariable UUID userId,
            @Valid @RequestBody ConfirmDebitRequest request) {
        log.info("Solicitud de confirmación de débito para userId: {}, monto: {}", userId, request.amount());
        WalletResponse wallet = walletService.confirmDebit(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Débito confirmado exitosamente", wallet));
    }

    @Operation(summary = "Acreditar fondos", description = "Acredita fondos a la wallet")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<WalletResponse>> credit(
            @PathVariable UUID userId,
            @Valid @RequestBody CreditRequest request) {
        log.info("Solicitud de crédito para userId: {}, monto: {}", userId, request.amount());
        WalletResponse wallet = walletService.credit(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Crédito aplicado exitosamente", wallet));
    }

    @Operation(summary = "Crear wallet (interno)", description = "Endpoint interno para crear una wallet")
    @PostMapping("/internal/wallets")
    public ResponseEntity<ApiResponse<Void>> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        log.info("Solicitud de creación de wallet para userId: {}", request.userId());
        walletService.createWalletIfNotExists(request.userId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet creada exitosamente"));
    }

    @Operation(summary = "Obtener mi wallet", description = "Devuelve la wallet del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        log.info("Solicitud de wallet propia para userId: {}", userId);
        WalletResponse wallet = walletService.getWallet(userId);
        log.info("Wallet obtenida: {}", wallet);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    /**
     * Extrae el userId del JWT
     */
    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }
}
