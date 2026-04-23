package org.example.notificationservice.config;

import java.time.LocalDateTime;
import java.util.List;

import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.enums.NotificationStatus;
import org.example.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Scheduled(fixedDelay = 2000)
    public void processPendingNotifications() {
        List<Notification> toProcess = notificationRepository
                .findByStatusIn(List.of(NotificationStatus.PENDING, NotificationStatus.FAILED));

        if (toProcess.isEmpty()) return;

        log.info("Processing {} pending notifications", toProcess.size());

        for (Notification notification : toProcess) { 
            try {
                sendEmail(notification.getToCustomerEmail(), notification.getMessage());

                notification.setStatus(NotificationStatus.SENT);
                notification.setProcessedAt(LocalDateTime.now());
                notificationRepository.save(notification);

            } catch (Exception e) {
                log.error("Failed to send notification id={}", notification.getId(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setProcessedAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        }
    }

    private void sendEmail(String toEmail, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("Notification");
        mailMessage.setText(message);
        mailSender.send(mailMessage);
        log.info("Email sent to {}", toEmail);
    }
}