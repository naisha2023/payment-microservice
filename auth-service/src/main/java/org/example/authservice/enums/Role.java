package org.example.authservice.enums;

public enum Role {
    CUSTOMER("Usuario final que puede enviar y recibir dinero."),
    SUPPORT("Personal de soporte que puede ver transacciones."),
    COMPLIANCE_OFFICER("Revisión AML"),
    ADMIN("Administrador técnico"),
    SYSTEM_SERVICE("Comunicación entre microservicios");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
