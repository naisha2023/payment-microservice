package org.example.ledgerservice.enums;

public enum EntryType {
    DEBIT("Debit"),
    CREDIT("Credit");

    private final String displayName;

    EntryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
