package org.example.shared.event;

import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String currency
) {}