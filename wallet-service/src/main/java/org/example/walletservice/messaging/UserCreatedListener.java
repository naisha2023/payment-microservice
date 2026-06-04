package org.example.walletservice.messaging;

import org.example.shared.event.UserCreatedEvent;
import org.example.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.example.shared.config.KafkaTopics;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final WalletService walletService;

    @KafkaListener(topics = KafkaTopics.USER_CREATED, groupId = "wallet-service")
    public void handle(UserCreatedEvent event) {
        log.info("UserCreatedEvent recibido: {}", event.userId());
        walletService.createWalletIfNotExists(event.userId());
    }
}