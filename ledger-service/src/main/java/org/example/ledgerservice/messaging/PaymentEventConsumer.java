package org.example.ledgerservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.example.ledgerservice.dto.PaymentCreatedEvent;
import org.example.ledgerservice.service.LedgerTransactionServiceImpl;

import org.slf4j.MDC;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final LedgerTransactionServiceImpl ledgerEventService;
    private final ObjectMapper objectMapper;

    @RabbitListener(
        queues = "payment.created.queue",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void handle(
            String payload,
            @Header(name = "X-Correlation-ID", required = false)
            String correlationId
    ) {

        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlation_id", correlationId);
        }

        try {

            log.info(
                    "payment_created_event_received correlationId={}",
                    correlationId
            );

            PaymentCreatedEvent event =
                    objectMapper.readValue(payload, PaymentCreatedEvent.class);

            ledgerEventService.handlePaymentCreated(event);

            log.info(
                    "payment_created_event_processed paymentId={} walletId={} amount={} correlationId={}",
                    event.paymentId(),
                    event.walletId(),
                    event.amount(),
                    correlationId
            );

        } catch (Exception ex) {

            log.error(
                    "payment_created_event_processing_failed correlationId={} reason={}",
                    correlationId,
                    ex.getMessage(),
                    ex
            );

            throw new RuntimeException(ex);

        } finally {
            MDC.remove("correlation_id");
        }
    }
}