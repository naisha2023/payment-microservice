package org.example.paymentservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 2000)
    public void publish() {
        var events = outboxRepository.findByPublishedFalse();

        for (var event : events) {
    try {
        MessageBuilder<String> messageBuilder = MessageBuilder
                .withPayload(event.getPayload())
                .setHeader(KafkaHeaders.TOPIC, event.getType())
                .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString());

        if (event.getCorrelationId() != null) {
            messageBuilder.setHeader("X-Correlation-ID", event.getCorrelationId());
        }

        kafkaTemplate.send(messageBuilder.build()).get();

        event.setPublished(true);
        outboxRepository.save(event);

        log.info(
                "Event published: id={}, topic={}",
                event.getId(),
                event.getType()
        );

    } catch (Exception e) {
        log.error("Error publishing event {}", event.getId(), e);
    }
}
    }
}