package org.example.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest(
        BigDecimal amount,
        String reference,
        String description,
        UUID paymentId
) {}