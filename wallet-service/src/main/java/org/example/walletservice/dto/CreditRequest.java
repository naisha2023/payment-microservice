package org.example.walletservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreditRequest(
        @NotNull String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
