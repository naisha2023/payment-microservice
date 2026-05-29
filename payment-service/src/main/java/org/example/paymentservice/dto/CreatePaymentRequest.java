package org.example.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.paymentservice.enums.PaymentType;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        String currency,

        String description,
        
        @NotNull
        PaymentType paymentType
) {}