package org.example.notificationservice.repository;

import java.util.List;

import org.example.notificationservice.entity.Notification;
import org.example.notificationservice.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByStatus(NotificationStatus status);
    
    List<Notification> findByStatusIn(List<NotificationStatus> statuses);
}