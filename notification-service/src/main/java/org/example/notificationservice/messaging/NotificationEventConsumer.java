package org.example.notificationservice.messaging;

import org.example.notificationservice.service.NotificationService;
import org.example.shared.dtos.PaymentCreatedEvent;
import org.example.shared.event.NotificationEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "notification.created.queue",
        containerFactory = "rabbitListenerContainerFactory"
    )
     public void handle(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            notificationService.send(event);
            log.info("Notification event processed successfully: {}", event);
        } catch (Exception e) {
            log.error("Error occurred while handling notification event", e);
            throw new RuntimeException(e);
        }
    }
}
