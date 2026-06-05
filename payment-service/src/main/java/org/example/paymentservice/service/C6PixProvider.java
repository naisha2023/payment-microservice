package org.example.paymentservice.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.example.paymentservice.dto.PixProviderResponse;
import org.example.paymentservice.interfaces.PixProvider;
import org.example.paymentservice.dto.ProviderPaymentStatus;

public class C6PixProvider implements PixProvider{

    @Override
    public PixProviderResponse createPixCharge(UUID paymentId, BigDecimal amount) {
        
        
        return null;
    }

    @Override
    public ProviderPaymentStatus getPaymentStatus(String providerPaymentId) {
        return null;
    }
}
