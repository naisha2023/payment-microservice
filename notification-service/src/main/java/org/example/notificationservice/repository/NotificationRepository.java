package org.example.notificationservice.repository;

import java.util.UUID;

import org.example.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}