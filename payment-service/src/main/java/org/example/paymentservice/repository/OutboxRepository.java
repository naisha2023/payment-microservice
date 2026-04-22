package org.example.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.example.paymentservice.entity.OutboxEvent;

public interface OutboxRepository extends JpaRepository<OutboxEvent, java.util.UUID> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
    List<OutboxEvent> findByPublishedFalse();
}
