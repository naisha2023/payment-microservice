package org.example.paymentservice.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.example.paymentservice.dto.PixProviderResponse;
import org.example.paymentservice.interfaces.PixProvider;

public class C6PixProvider implements PixProvider{

    @Override
    public PixProviderResponse createPixCharge(UUID paymentId, BigDecimal amount) {
        
        return new PixProviderResponse(
            response.getTransactionId(),
            response.getPixCopyPaste(),
            response.getQrCodeBase64(),
            response.getExpirationDate()
        );
    }
}
