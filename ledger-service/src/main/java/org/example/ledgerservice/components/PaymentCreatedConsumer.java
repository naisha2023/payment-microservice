package org.example.ledgerservice.components;

import org.example.ledgerservice.dto.PaymentCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedConsumer {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "payment.created.queue")
    public void handle(String payload) {
        try {
            PaymentCreatedEvent event = objectMapper.readValue(payload, PaymentCreatedEvent.class);
            log.info("Payment recibido: {}", event);
        } catch (Exception e) {
            log.error("Error deserializando evento", e);
        }
    }
}