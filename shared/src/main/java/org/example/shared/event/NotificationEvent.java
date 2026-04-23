package org.example.shared.event;

import java.util.UUID;

public record NotificationEvent(
        UUID toCustomerId,
        UUID paymentId,
        String message
) {}