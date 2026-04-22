package org.example.paymentservice.enums;

public enum EventType {
    PAYMENT_CREATED("payment.created"),
    PAYMENT_UPDATED("payment.updated"),
    PAYMENT_DELETED("payment.deleted"),
    PAYMENT_COMPLETED("payment.completed");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}