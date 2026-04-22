package org.example.ledgerservice.events;

import java.time.LocalDateTime;
import java.util.List;

import org.example.ledgerservice.entity.OutboxEvent;
import org.example.ledgerservice.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    public static final String LEDGER_EXCHANGE = "ledger.exchange";

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        LEDGER_EXCHANGE,
                        event.getEventType(),
                        event.getPayload()
                );

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Outbox event published successfully: {}", event.getId());

            } catch (Exception e) {
                log.error("Error publishing outbox event {}", event.getId(), e);
            }
        }
    }
}