package org.example.walletservice.messaging;

import lombok.RequiredArgsConstructor;
import org.example.shared.config.KafkaTopics;
import org.example.shared.event.WalletCreatedEvent;
import org.example.shared.event.WalletDebitConfirmedEvent;
import org.example.shared.event.WalletFundedEvent;
import org.example.shared.event.WalletReleaseFundedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(WalletCreatedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WALLET_CREATED,
                event.walletId().toString(),
                event
        );
    }

    public void publish(WalletFundedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WALLET_FUNDED,
                event.walletId().toString(),
                event
        );
    }

    public void publish(WalletDebitConfirmedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WALLET_DEBIT_CONFIRMED,
                event.walletId().toString(),
                event
        );
    }

    public void publish(WalletReleaseFundedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.WALLET_RELEASE_FUNDED,
                event.walletId().toString(),
                event
        );
    }
}