package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.example.paymentservice.client.WalletClient;
import org.example.paymentservice.constants.PaymentConstants;
import org.example.paymentservice.dto.CreatePaymentRequest;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.dto.PixProviderResponse;
import org.example.paymentservice.dto.WalletOperationRequest;
import org.example.paymentservice.dto.WalletResponse;
import org.example.paymentservice.entity.OutboxEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.EventType;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.enums.PaymentType;
import org.example.paymentservice.exception.InvalidPaymentStateException;
import org.example.paymentservice.exception.PaymentNotAuthorizedException;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.exception.WalletServiceUnavailableException;
import org.example.paymentservice.repository.OutboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.shared.config.KafkaTopics;
import org.example.shared.dtos.ApiResponse;
import org.example.shared.dtos.PaymentCompletedEvent;
import org.example.shared.dtos.PaymentCreatedEvent;
import org.example.shared.event.NotificationEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletClient walletClient;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentFailureService paymentFailureService;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, Jwt jwt, String authHeader) {
        UUID userId = extractUserId(jwt);

        log.info(
                "payment_create_started userId={} amount={} currency={} paymentType={} description={}", 
                userId,
                request.amount(),
                request.currency(),
                request.paymentType(),
                request.description()
        );

        ApiResponse<WalletResponse> walletResponse = walletClient.getMyWallet(authHeader);

        if (walletResponse == null) {
            log.error("wallet_lookup_failed userId={} reason=null_wallet_response", userId);
            throw new IllegalStateException("La respuesta del wallet-service fue nula para el usuario " + userId);
        }

        if (!walletResponse.isSuccess()) {
            log.error("wallet_lookup_failed userId={} reason=wallet_service_error", userId);
            throw new IllegalStateException("wallet-service respondió con error al consultar la wallet del usuario " + userId);
        }

        WalletResponse wallet = walletResponse.getData();

        if (wallet == null || wallet.id() == null) {
            log.error("wallet_lookup_failed userId={} reason=invalid_wallet_data", userId);
            throw new IllegalStateException("No se pudo obtener una wallet válida para el usuario " + userId);
        }

        log.info(
                "wallet_resolved userId={} walletId={}",
                userId,
                wallet.id()
        );

        Payment payment = buildPayment(request, userId, wallet.id());
        payment = paymentRepository.save(payment);

        log.info(
                "payment_persisted paymentId={} userId={} walletId={} amount={} currency={} status={}",
                payment.getId(),
                payment.getUserId(),
                payment.getWalletId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus()
        );

        try {
            reserveFundsInWallet(payment, userId, authHeader);

            payment.setStatus(PaymentStatus.RESERVED);
            payment = paymentRepository.save(payment);

            publishPaymentCreatedEvent(payment, wallet.id());

            log.info(
                    "payment_reserved paymentId={} userId={} walletId={} amount={} currency={}",
                    payment.getId(),
                    payment.getUserId(),
                    payment.getWalletId(),
                    payment.getAmount(),
                    payment.getCurrency()
            );

            return toResponse(payment);

        } catch (Exception ex) {
            log.error(
                    "payment_create_failed paymentId={} userId={} reason={}",
                    payment.getId(),
                    payment.getUserId(),
                    ex.getMessage(),
                    ex
            );

            paymentFailureService.markPaymentAsFailed(payment.getId(), ex);

            throw ex;
        }
    }

    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, Jwt jwt, String authHeader) {
        UUID userId = extractUserId(jwt);

        log.info(
                "payment_confirm_started paymentId={} userId={}",
                paymentId,
                userId
        );

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);
        validatePaymentIsReserved(payment);

        try {
            confirmDebitInWallet(payment, userId, authHeader);

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpdatedAt(Instant.now());
            payment = paymentRepository.save(payment);

            publishPaymentCompletedEvent(payment);

            log.info(
                    "payment_completed paymentId={} userId={} walletId={} amount={} currency={}",
                    payment.getId(),
                    payment.getUserId(),
                    payment.getWalletId(),
                    payment.getAmount(),
                    payment.getCurrency()
            );

            return toResponse(payment);

        } catch (Exception ex) {
            log.error(
                    "payment_confirm_failed paymentId={} userId={} reason={}",
                    paymentId,
                    userId,
                    ex.getMessage(),
                    ex
            );

            paymentFailureService.markPaymentAsFailed(payment.getId(), ex);
            throw ex;
        }
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, Jwt jwt, String authHeader) {
        UUID userId = extractUserId(jwt);

        log.info(
                "payment_cancel_started paymentId={} userId={}",
                paymentId,
                userId
        );

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);
        validatePaymentNotCompleted(payment);

        if (payment.getStatus() == PaymentStatus.RESERVED) {
            releaseFundsInWallet(payment, userId, authHeader);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info(
                "payment_cancelled paymentId={} userId={} walletId={} amount={} currency={}",
                payment.getId(),
                payment.getUserId(),
                payment.getWalletId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        return toResponse(payment);
    }

    public PaymentResponse getById(UUID paymentId, Jwt jwt) {
        UUID userId = extractUserId(jwt);

        log.info(
                "payment_get_by_id_started paymentId={} userId={}",
                paymentId,
                userId
        );

        Payment payment = findPaymentById(paymentId);
        validatePaymentOwnership(payment, userId);

        log.info(
                "payment_get_by_id_success paymentId={} userId={} status={}",
                payment.getId(),
                payment.getUserId(),
                payment.getStatus()
        );

        return toResponse(payment);
    }

    public List<PaymentResponse> getMyPayments(Jwt jwt) {
        UUID userId = extractUserId(jwt);

        log.info(
                "payment_list_started userId={}",
                userId
        );

        List<PaymentResponse> payments = paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        log.info(
                "payment_list_success userId={} count={}",
                userId,
                payments.size()
        );

        return payments;
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        log.info(
                "payment_outbox_create_started paymentId={} routingKeys={},{}",
                payment.getId(),
                KafkaTopics.PAYMENT_PROCESSED,
                KafkaTopics.NOTIFICATION_CREATED
        );

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
                KafkaTopics.PAYMENT_PROCESSED,
                correlationId()
        ));

        outboxEvents.add(new OutboxEvent(
                null,
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_MESSAGE_SEND,
                toJson(notificationEvent),
                Instant.now(),
                false,
                KafkaTopics.NOTIFICATION_CREATED,
                correlationId()
        ));

        outboxRepository.saveAll(outboxEvents);

        log.info(
                "payment_outbox_created paymentId={} eventCount={} outboxEventIds={}",
                payment.getId(),
                outboxEvents.size(),
                outboxEvents.stream().map(OutboxEvent::getId).toList()
        );
    }

    private void publishPaymentCreatedEvent(Payment payment, UUID walletId) {
        log.info(
                "payment_outbox_create_started paymentId={} routingKey={}",
                payment.getId(),
                KafkaTopics.PAYMENT_CREATED
        );

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
                KafkaTopics.PAYMENT_CREATED,
                correlationId()
        );

        outboxRepository.save(outbox);

        log.info(
                "payment_outbox_created paymentId={} outboxEventId={} routingKey={}",
                payment.getId(),
                outbox.getId(),
                KafkaTopics.PAYMENT_CREATED
        );
    }

    private String correlationId() {
        return MDC.get("correlation_id");
    }

    private Payment buildPayment(CreatePaymentRequest request, UUID userId, UUID walletId) {
        return Payment.builder()
                .walletId(walletId)
                .userId(userId)
                .reference(UUID.randomUUID().toString())
                .amount(request.amount())
                .currency(request.currency())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .paymentType(request.paymentType())
                .build();
    }

    @CircuitBreaker(name = "wallet-service", fallbackMethod = "reserveFallback")
    @Retry(name = "wallet-service")
    private void reserveFundsInWallet(Payment payment, UUID userId, String authHeader) {
        log.info(
                "wallet_reserve_requested paymentId={} userId={} walletId={} amount={} currency={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId()
        );

        walletClient.reserve(userId, request, authHeader);

        log.info(
                "wallet_reserve_success paymentId={} walletId={} amount={}",
                payment.getId(),
                payment.getWalletId(),
                payment.getAmount()
        );
    }

    @CircuitBreaker(name = "wallet-service", fallbackMethod = "confirmDebitFallback")
    @Retry(name = "wallet-service")
    private void confirmDebitInWallet(Payment payment, UUID userId, String authHeader) {
        log.info(
                "wallet_confirm_debit_requested paymentId={} userId={} walletId={} amount={} currency={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId()
        );

        walletClient.confirmDebit(userId, request, authHeader);

        log.info(
                "wallet_confirm_debit_success paymentId={} walletId={} amount={}",
                payment.getId(),
                payment.getWalletId(),
                payment.getAmount()
        );
    }

    @CircuitBreaker(name = "wallet-service", fallbackMethod = "releaseFallback")
    @Retry(name = "wallet-service")
    private void releaseFundsInWallet(Payment payment, UUID userId, String authHeader) {
        log.info(
                "wallet_release_requested paymentId={} userId={} walletId={} amount={} currency={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        WalletOperationRequest request = new WalletOperationRequest(
                payment.getAmount(),
                payment.getReference(),
                payment.getDescription(),
                payment.getId()
        );

        walletClient.release(userId, request, authHeader);

        log.info(
                "wallet_release_success paymentId={} walletId={} amount={}",
                payment.getId(),
                payment.getWalletId(),
                payment.getAmount()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentAsFailed(Payment payment, Exception ex) {
        log.error(
                "payment_failed paymentId={} userId={} amount={} currency={} reason={}",
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                ex.getMessage(),
                ex
        );

        payment.setStatus(PaymentStatus.REJECTED);
        payment.setFailureReason(truncate(
                PaymentConstants.ERROR_PROCESSING_PAYMENT + ": " + ex.getMessage(),
                PaymentConstants.MAX_FAILURE_REASON_LENGTH
        ));

        paymentRepository.save(payment);
    }

    private void validatePaymentOwnership(Payment payment, UUID userId) {
        if (!payment.getUserId().equals(userId)) {
            log.warn(
                    "payment_ownership_validation_failed paymentId={} ownerUserId={} requesterUserId={}",
                    payment.getId(),
                    payment.getUserId(),
                    userId
            );

            throw new PaymentNotAuthorizedException(PaymentConstants.ERROR_PAYMENT_NOT_AUTHORIZED);
        }
    }

    private void validatePaymentIsReserved(Payment payment) {
        if (payment.getStatus() != PaymentStatus.RESERVED) {
            log.warn(
                    "payment_state_validation_failed paymentId={} expectedStatus={} actualStatus={}",
                    payment.getId(),
                    PaymentStatus.RESERVED,
                    payment.getStatus()
            );

            throw new InvalidPaymentStateException(PaymentConstants.ERROR_PAYMENT_NOT_RESERVED);
        }
    }

    private void validatePaymentNotCompleted(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn(
                    "payment_cancel_validation_failed paymentId={} status={}",
                    payment.getId(),
                    payment.getStatus()
            );

            throw new InvalidPaymentStateException(PaymentConstants.ERROR_PAYMENT_ALREADY_COMPLETED);
        }
    }

    private Payment findPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.warn("payment_not_found paymentId={}", paymentId);

                    return new PaymentNotFoundException(
                            PaymentConstants.ERROR_PAYMENT_NOT_FOUND + ": " + paymentId
                    );
                });
    }

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
                payment.getUpdatedAt(),
                payment.getQrCode(),
                payment.getQrCodeBase64()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            log.error(
                    "json_serialization_failed objectType={} reason={}",
                    obj == null ? "null" : obj.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );

            throw new RuntimeException("Error al serializar objeto", ex);
        }
    }

    private String truncate(String value, int max) {
        if (value == null)
            return null;

        return value.length() <= max ? value : value.substring(0, max);
    }

    protected void reserveFallback(
            Payment payment,
            UUID userId,
            String authHeader,
            Exception ex
    ) {
        log.error(
                "wallet_reserve_fallback paymentId={} userId={} walletId={} reason={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                ex.getMessage(),
                ex
        );

        throw new WalletServiceUnavailableException(
                "Wallet service no disponible. No se pudo procesar el pago.",
                ex
        );
    }

    protected void confirmDebitFallback(
            Payment payment,
            UUID userId,
            String authHeader,
            Exception ex
    ) {
        log.error(
                "wallet_confirm_debit_fallback paymentId={} userId={} walletId={} reason={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                ex.getMessage(),
                ex
        );

        throw new WalletServiceUnavailableException(
                "Wallet service no disponible. El débito no pudo confirmarse.",
                ex
        );
    }

    protected void releaseFallback(
            Payment payment,
            UUID userId,
            String authHeader,
            Exception ex
    ) {
        log.error(
                "wallet_release_fallback paymentId={} userId={} walletId={} reason={}",
                payment.getId(),
                userId,
                payment.getWalletId(),
                ex.getMessage(),
                ex
        );

        throw new WalletServiceUnavailableException(
                "Wallet service no disponible. Los fondos no pudieron liberarse.",
                ex
        );
    }

    @Transactional
        public void confirmPixDeposit(String providerPaymentId, BigDecimal amount, Jwt jwt) {
        Payment payment = paymentRepository
                .findByProviderPaymentIdForUpdate(providerPaymentId)
                .orElseThrow();

        if (payment.getStatus() == PaymentStatus.APPROVED) {
                return;
        }

        payment.markApproved();

        WalletOperationRequest walletRequest = new WalletOperationRequest(
                amount,
                payment.getReference(),
                payment.getDescription(),
                payment.getId()
        );

        walletClient.deposit(
                payment.getUserId(),
                walletRequest,
                jwt.getTokenValue()
        );

        paymentRepository.save(payment);

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_PAYMENT_COMPLETED,
                toJson(new PaymentCompletedEvent(
                        UUID.randomUUID(),
                        EventType.PAYMENT_COMPLETED.getDescription(),
                        Instant.now(),
                        payment.getId(),
                        payment.getWalletId(),
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getStatus().name()
                )),
                Instant.now(),
                false,
                KafkaTopics.PAYMENT_PROCESSED,
                correlationId()
        );

        outboxRepository.save(event);

        

        OutboxEvent notificationEvent = new OutboxEvent(
                UUID.randomUUID(),
                PaymentConstants.OUTBOX_AGGREGATE_TYPE,
                payment.getId().toString(),
                PaymentConstants.EVENT_MESSAGE_SEND,
                toJson(new NotificationEvent(
                        payment.getUserId(),
                        payment.getId(),
                        EventType.PAYMENT_COMPLETED.getDescription()
                )),
                Instant.now(),
                false,
                KafkaTopics.NOTIFICATION_CREATED,
                correlationId()
        );

        outboxRepository.save(notificationEvent);

        log.info(
                "payment_approved paymentId={} userId={} amount={} currency={}",
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency()
        );
    }

        @Transactional
        public Payment createPendingPixDeposit(Jwt jwt, BigDecimal amount) {

                UUID userId = extractUserId(jwt);

                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Amount must be greater than zero");
                }

                Payment payment = new Payment();

                payment.setId(UUID.randomUUID());
                payment.setUserId(userId);
                payment.setAmount(amount);
                payment.setPaymentType(PaymentType.PIX);
                payment.setStatus(PaymentStatus.PENDING);
                payment.setCreatedAt(Instant.now());
                payment.setUpdatedAt(Instant.now());
                payment.setReference("PIX-" + payment.getId());
                payment.setCurrency("BRL");
                payment.setDescription("Depósito via PIX");
                payment.setProvider("PIX_PROVIDER");

                return paymentRepository.save(payment);
        }

        @Transactional
        public Payment attachProviderData(
                UUID paymentId,
                PixProviderResponse pix
        ) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

                payment.setProvider("C6");
                payment.setProviderPaymentId(pix.providerPaymentId());
                payment.setQrCode(pix.qrCode());
                payment.setQrCodeBase64(pix.qrCodeBase64());

                return paymentRepository.save(payment);
        }
}