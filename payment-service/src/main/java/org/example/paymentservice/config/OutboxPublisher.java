package org.example.paymentservice.config;

import org.example.paymentservice.repository.OutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 2000)
    public void publish() {
        var events = outboxRepository.findByPublishedFalse();

        for (var event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        "payment.exchange",
                        "payment.created",
                        event.getPayload()
                );

                event.setPublished(true);
                outboxRepository.save(event);

                log.info("Evento publicado: {}", event.getId());

            } catch (Exception e) {
                log.error("Error publicando evento {}", event.getId(), e);
            }
        }
    }
}