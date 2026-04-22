package org.example.walletservice.messaging;

import org.example.shared.config.RabbitConfig;
import org.example.shared.event.UserCreatedEvent;
import org.example.walletservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final WalletService walletService;

    @RabbitListener(queues = RabbitConfig.USER_CREATED_QUEUE)
    public void handle(UserCreatedEvent event) {
        log.info("UserCreatedEvent recibido: {}", event.userId());
        walletService.createWalletIfNotExists(event.userId());
    }
}