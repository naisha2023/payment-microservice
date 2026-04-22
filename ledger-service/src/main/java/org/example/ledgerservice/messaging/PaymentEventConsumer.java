package org.example.ledgerservice.messaging;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import org.example.ledgerservice.dto.PaymentCreatedEvent;
import org.example.ledgerservice.service.LedgerTransactionServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final LedgerTransactionServiceImpl ledgerEventService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "payment.created.queue",
        containerFactory = "rabbitListenerContainerFactory"
    )
     public void handle(String payload) {
        try {
            PaymentCreatedEvent event = objectMapper.readValue(payload, PaymentCreatedEvent.class);
            ledgerEventService.handlePaymentCreated(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}