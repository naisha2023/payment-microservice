package org.example.ledgerservice.enums;

public enum TransactionType {
    RESERVE("Reserve"),
    CONFIRM_DEBIT("ConfirmDebit"),
    CONFIRM_CREDIT("ConfirmCredit"),
    RELEASE("Release"),
    REFUND("Refund"),
    ADJUSTMENT("Adjustment"),
    CREATED("Created"),
    FEE("Fee");

    private final String displayName;
    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
