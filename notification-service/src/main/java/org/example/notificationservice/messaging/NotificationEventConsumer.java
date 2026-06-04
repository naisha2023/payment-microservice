package org.example.notificationservice.messaging;

import org.example.notificationservice.service.NotificationService;
import org.example.shared.event.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "notification.created",
            groupId = "notification-service"
    )
    public void handle(NotificationEvent event) {

        try {

            notificationService.send(event);

            log.info(
                    "Notification event processed successfully: {}",
                    event
            );

        } catch (Exception e) {

            log.error(
                    "Error occurred while handling notification event",
                    e
            );

            throw e;
        }
    }
}