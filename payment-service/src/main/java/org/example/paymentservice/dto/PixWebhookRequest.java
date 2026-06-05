package org.example.paymentservice.dto;

public record PixWebhookRequest(
        String providerPaymentId
) {
}