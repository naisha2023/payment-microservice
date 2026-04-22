package org.example.ledgerservice.constants;

/**
 * Constantes utilizadas en el servicio de ledger
 */
public final class LedgerConstants {

    private LedgerConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    // Mensajes de error
    public static final String ERROR_LEGDGER_NOT_FOUND = "Cuenta ledger no encontrada para wallet";
    

    // Mensajes de éxito
    

    // Configuración por defecto
    public static final String DEFAULT_CURRENCY = "USD";
}
