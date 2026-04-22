package org.example.paymentservice.dto;

import org.example.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID userId,
        String reference,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}