package org.example.shared.enums;

public enum AccountType {
    CUSTOMER("Cuenta del usuario final (cliente)"),
    MERCHANT("Cuenta del comercio / receptor del pago"),
    SYSTEM("Cuenta interna del sistema"),
    FEE("Cuenta donde el sistema gana dinero"),
    RESERVE("Cuenta de fondos retenidos (hold / escrow)");

    private final String description;
    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
