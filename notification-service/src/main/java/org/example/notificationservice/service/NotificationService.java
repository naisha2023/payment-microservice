package org.example.notificationservice.service;

import java.time.LocalDateTime;

import org.example.notificationservice.client.AuthClient;
import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.enums.NotificationStatus;
import org.example.notificationservice.repository.NotificationRepository;
import org.example.shared.dtos.UserResponse;
import org.example.shared.event.NotificationEvent;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthClient authClient;

    public void send(NotificationEvent notificationRequest) {
    UserResponse user = authClient.findUserById(notificationRequest.toCustomerId()).getData();

    // Solo guarda como PENDING, el scheduler se encarga del envío
    notificationRepository.save(
        Notification.builder()
            .toCustomerId(notificationRequest.toCustomerId())
            .toCustomerEmail(user.email())
            .sender("Amigoscode")
            .message(notificationRequest.message())
            .sentAt(LocalDateTime.now())
            .status(NotificationStatus.PENDING) // 👈
            .build()
    );
    log.info("Notification queued for {}", user.email());
}
}