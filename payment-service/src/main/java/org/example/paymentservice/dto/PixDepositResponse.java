package org.example.paymentservice.dto;

import java.util.UUID;

public record PixDepositResponse(UUID paymentId, String providerPaymentId, String qrCode, String qrCodeBase64) {

}
