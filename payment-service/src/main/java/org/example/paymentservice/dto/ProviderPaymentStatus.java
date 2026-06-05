package org.example.paymentservice.dto;

import java.math.BigDecimal;

public record ProviderPaymentStatus(String status, String providerPaymentId, BigDecimal amount) {

}
