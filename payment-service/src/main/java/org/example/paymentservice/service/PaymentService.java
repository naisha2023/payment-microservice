package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.example.paymentservice.client.WalletClient;
import org.example.paymentservice.constants.PaymentConstants;
import org.example.paymentservice.dto.CreatePaymentRequest;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.dto.WalletOperationRequest;
import org.example.paymentservice.dto.WalletResponse;
import org.example.paymentservice.entity.OutboxEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.EventType;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.exception.InvalidPaymentStateException;
import org.example.paymentservice.exception.PaymentNotAuthorizedException;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.repository.OutboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.shared.config.RabbitConfig;
import org.example.shared.dtos.ApiResponse;
import org.example.shared.dtos.PaymentCompletedEvent;
import org.example.shared.dtos.PaymentCreatedEvent;
import org.example.shared.event.NotificationEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de pagos que maneja la creación, confirmación y cancelación de pagos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletClient walletClient;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentFailureService paymentFailureService;

    /**
     * Crea un nuevo pago y reserva fondos en la wallet
     */
    @Transactional
public PaymentResponse createPayment(CreatePaymentRequest request, Jwt jwt, String authHeader) {
    UUID userId = extractUserId(jwt);

    log.info("Creando pago para usuario: {}, monto: {}", userId, request.amount());

    ApiResponse<WalletResponse> walletResponse = walletClient.getMyWallet(authHeader);

    if (walletResponse == null) {
        throw new IllegalStateException("La respuesta del wallet-service fue nula para el usuario " + userId);
    }

    if (!walletResponse.isSuccess()) {
        throw new IllegalStateException("wallet-service respondió con error al consultar la wallet del usuario " + userId);
    }

    WalletResponse wallet = walletResponse.getData();

    if (wallet == null || wallet.id() == null) {
        throw new IllegalStateException("No se pudo obtener una wallet válida para el usuario " + userId);
    }

    log.info("Wallet válida obtenida para usuario {}: {}", userId, wallet.id());

    Payment payment = buildPayment(request, userId, wallet.id());
    payment = paymentRepository.save(payment);

    try {
        reserveFundsInWallet(payment, userId, authHeader);

        payment.setStatus(PaymentStatus.RESERVED);
        payment = paymentRepository.save(payment);

        publishPaymentCreatedEvent(payment, wallet.id());

        log.info("Pago procesado correctamente: {}", payment.getId());
        return toResponse(payment);

    } catch (Exception ex) {
        log.error("Error al crear el pago {}: {}", payment.getId(), ex.getMessage(), ex);

        paymentFailureService.markPaymentAsFailed(payment.getId(), ex);

        throw ex;
    }
}

    /**
     * Confirma un pago y deduce los fondos de la wallet
     */
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, Jwt jwt, String authHeader) {
        UUID userId = extractUserId(jwt);
        log.info("Confirmando pago: {} para usuario: {}", paymentId, userId);

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);
        validatePaymentIsReserved(payment);

        try {
            confirmDebitInWallet(payment, userId, authHeader);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(Instant.now());
            payment = paymentRepository.save(payment);

            publishPaymentCompletedEvent(payment);

            log.info("Pago confirmado exitosamente: {}", paymentId);
            return toResponse(payment);

        } catch (Exception ex) {
            log.error("Error al confirmar pago {}: {}", paymentId, ex.getMessage(), ex);
            paymentFailureService.markPaymentAsFailed(payment.getId(), ex);
            throw ex;
        }
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        log.info("Guardando evento PaymentCompleted en outbox para pago: {}", payment.getId());

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
                UUID.randomUUID(),
                EventType.PAYMENT_COMPLETED.getDescription(),
                Instant.now(),
                payment.getId(),
                payment.getWalletId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name()
        );

        NotificationEvent notificationEvent = new NotificationEvent(
                payment.getUserId(),
                payment.getId(),
                EventType.PAYMENT_COMPLETED.getDescription()
        );

        List<OutboxEvent> outboxEvents = new ArrayList<>();
        outboxEvents.add(new OutboxEvent(
                null,
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_PAYMENT_COMPLETED,
                toJson(paymentEvent),
                Instant.now(),
                false,
                RabbitConfig.PAYMENT_PROCESSED_ROUTING_KEY
        ));

        outboxEvents.add(new OutboxEvent(
                null,
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_MESSAGE_SEND,
                toJson(notificationEvent),
                Instant.now(),
                false,
                RabbitConfig.NOTIFICATION_CREATED_ROUTING_KEY
        ));

        outboxRepository.saveAll(outboxEvents);

        log.info("eventos en outbox a ser publicados: {}", outboxEvents.stream().map(OutboxEvent::getId).toList());

        log.info("notificacion enviada correctamente");
        log.info("Evento PaymentCompleted guardado correctamente en outbox para pago: {}", payment.getId());
    }

    /**
     * Cancela un pago y libera los fondos reservados
     */
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, Jwt jwt, String authHeader) {
        UUID userId = extractUserId(jwt);
        log.info("Cancelando pago: {} para usuario: {}", paymentId, userId);

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);
        validatePaymentNotCompleted(payment);

        if (payment.getStatus() == PaymentStatus.RESERVED) {
            releaseFundsInWallet(payment, userId, authHeader);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info("Pago cancelado exitosamente: {}", paymentId);
        return toResponse(payment);
    }

    /**
     * Obtiene un pago por su ID
     */
    public PaymentResponse getById(UUID paymentId, Jwt jwt) {
        UUID userId = extractUserId(jwt);
        log.info("Obteniendo pago: {} para usuario: {}", paymentId, userId);

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);

        return toResponse(payment);
    }

    /**
     * Obtiene todos los pagos de un usuario
     */
    public List<PaymentResponse> getMyPayments(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        log.info("Obteniendo pagos para usuario: {}", userId);

        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ==================== Métodos privados de operaciones ====================

    private Payment buildPayment(CreatePaymentRequest request, UUID userId, UUID walletId) {
        return Payment.builder()
                .walletId(walletId)
                .userId(userId)
                .reference(UUID.randomUUID().toString())
                .amount(request.amount())
                .currency(request.currency())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .build();
    }

    private void reserveFundsInWallet(Payment payment, UUID userId, String authHeader) {
        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId());
        walletClient.reserve(userId, request, authHeader);
        log.debug("Fondos reservados en wallet para pago: {}", payment.getId());
    }

    private void confirmDebitInWallet(Payment payment, UUID userId, String authHeader) {
        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId());
        walletClient.confirmDebit(userId, request, authHeader);
        log.debug("Débito confirmado en wallet para pago: {}", payment.getId());
    }

    private void releaseFundsInWallet(Payment payment, UUID userId, String authHeader) {
        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId());
        walletClient.release(userId, request, authHeader);
        log.debug("Fondos liberados en wallet para pago: {}", payment.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentAsFailed(Payment payment, Exception ex) {
        log.error("Error al procesar pago: {}", payment.getId(), ex);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(truncate(
                PaymentConstants.ERROR_PROCESSING_PAYMENT + ": " + ex.getMessage(),
                PaymentConstants.MAX_FAILURE_REASON_LENGTH));

        paymentRepository.save(payment);
    }

    private void publishPaymentCreatedEvent(Payment payment, UUID walletId) {
        log.info("Guardando evento PaymentCreated en outbox para pago: {}", payment.getId());

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID(),
                EventType.PAYMENT_CREATED.getDescription(),
                Instant.now(),
                payment.getId(),
                walletId,
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name()
        );

        OutboxEvent outbox = new OutboxEvent(
                null,
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_PAYMENT_CREATED,
                toJson(event),
                Instant.now(),
                false,
                RabbitConfig.PAYMENT_CREATED_ROUTING_KEY
        );

        outboxRepository.save(outbox);

        log.info("Evento PaymentCreated guardado correctamente en outbox para pago: {}", payment.getId());
    }

    // ==================== Métodos privados de validación ====================

    private void validatePaymentOwnership(Payment payment, UUID userId) {
        if (!payment.getUserId().equals(userId)) {
            log.warn("Usuario {} intentó acceder al pago {} que no le pertenece", userId, payment.getId());
            throw new PaymentNotAuthorizedException(PaymentConstants.ERROR_PAYMENT_NOT_AUTHORIZED);
        }
    }

    private void validatePaymentIsReserved(Payment payment) {
        if (payment.getStatus() != PaymentStatus.RESERVED) {
            log.warn("Intento de confirmar pago {} en estado: {}", payment.getId(), payment.getStatus());
            throw new InvalidPaymentStateException(PaymentConstants.ERROR_PAYMENT_NOT_RESERVED);
        }
    }

    private void validatePaymentNotCompleted(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("Intento de cancelar pago completado: {}", payment.getId());
            throw new InvalidPaymentStateException(PaymentConstants.ERROR_PAYMENT_ALREADY_COMPLETED);
        }
    }

    // ==================== Métodos privados de búsqueda ====================

    private Payment findPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        PaymentConstants.ERROR_PAYMENT_NOT_FOUND + ": " + paymentId));
    }

    // ==================== Métodos privados de utilidad ====================

    private UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("userId"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getReference(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error al serializar objeto a JSON", e);
            throw new RuntimeException("Error al serializar objeto", e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null)
            return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
