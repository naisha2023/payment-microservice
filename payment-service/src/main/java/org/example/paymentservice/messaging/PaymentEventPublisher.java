package org.example.paymentservice.messaging;

import lombok.RequiredArgsConstructor;
import org.example.paymentservice.config.RabbitConfig;
import org.example.shared.dtos.PaymentCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCreated(PaymentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENT_CREATED_KEY,
                event
        );
    }
}