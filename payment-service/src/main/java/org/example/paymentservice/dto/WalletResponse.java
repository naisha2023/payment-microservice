package org.example.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        String currency,
        BigDecimal balance
) {}
    