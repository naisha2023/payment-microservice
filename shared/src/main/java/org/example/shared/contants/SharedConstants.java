package org.example.shared.contants;

public class SharedConstants {
    private SharedConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    // Configuración por defecto
    public static final String WALLET_CREATED_ROUTING_KEY = "wallet.created";
}
