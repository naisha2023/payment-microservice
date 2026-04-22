package org.example.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ReserveFundsRequest(
        @NotNull UUID paymentId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}