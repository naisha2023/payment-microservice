package org.example.notificationservice.messaging;

import org.example.notificationservice.config.RabbitConfig;
import org.example.notificationservice.dto.LedgerTransactionCreatedEvent;
import org.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = RabbitConfig.NOTIFICATION_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handle(LedgerTransactionCreatedEvent event) {
        System.out.println("Notification event received: " + event);
        notificationService.createNotification(event);
    }
}