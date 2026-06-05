package org.example.paymentservice.interfaces;

import org.example.paymentservice.dto.PixProviderResponse;
import org.example.paymentservice.dto.ProviderPaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface PixProvider {
    PixProviderResponse createPixCharge( UUID paymentId, BigDecimal amount);
    ProviderPaymentStatus getPaymentStatus(String providerPaymentId);
}
