package org.example.paymentservice.enums;

public enum PaymentType {
    DEBIT_CARD("Debit card"),
    CREDIT_CARD("Credit card"),
    BANK_TRANSFER("Bank transfer"),
    DIGITAL_WALLET("Digital wallet"),
    PiX("Pix"),
    PAYPAL("PayPal");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
