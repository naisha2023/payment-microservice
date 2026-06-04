package org.example.authservice.messaging;

import org.example.shared.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;

import static org.example.shared.config.KafkaTopics.topic;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UserCreatedEvent event) {
        kafkaTemplate.send(topic, event.userId().toString(), event);
    }
}