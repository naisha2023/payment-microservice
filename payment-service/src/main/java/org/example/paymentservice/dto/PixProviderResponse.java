package org.example.paymentservice.dto;

import java.time.Instant;

public record PixProviderResponse(
        String providerPaymentId,
        String qrCode,
        String qrCodeBase64,
        Instant expiresAt
) {
}