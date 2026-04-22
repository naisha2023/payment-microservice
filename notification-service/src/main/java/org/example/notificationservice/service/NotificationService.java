package org.example.notificationservice.service;

import org.example.notificationservice.dto.LedgerTransactionCreatedEvent;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotification(LedgerTransactionCreatedEvent event) {
        Notification notification = new Notification();
        notification.setEventType("ledger.transaction.created");
        notification.setReferenceId(event.referenceId());
        notification.setStatus("CREATED");
        notification.setMessage(
                "Transaction %s created for %s %s"
                        .formatted(event.referenceId(), event.amount(), event.currency())
        );

        notificationRepository.save(notification);
    }
}