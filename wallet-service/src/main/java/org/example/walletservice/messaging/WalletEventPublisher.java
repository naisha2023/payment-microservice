package org.example.walletservice.messaging;

import org.example.shared.config.RabbitConfig;

import org.example.shared.event.WalletCreatedEvent;
import org.example.shared.event.WalletFundedEvent;
import org.example.shared.event.WalletReleaseFundedEvent;
import org.example.shared.event.WalletDebitConfirmedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(WalletCreatedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.WALLET_CREATED_ROUTING_KEY,
            event
        );
    }

    public void publish(WalletFundedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.WALLET_FUNDED_ROUTING_KEY,
            event
        );
    }

    public void publish(WalletDebitConfirmedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.WALLET_DEBIT_CONFIRMED_ROUTING_KEY,
            event
        );
    }

    public void publish(WalletReleaseFundedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.WALLET_RELEASE_FUNDED_ROUTING_KEY,
            event
        );
    }
}