package org.example.walletservice.constants;

/**
 * Constantes utilizadas en el servicio de wallet
 */
public final class WalletConstants {

    private WalletConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    // Mensajes de error
    public static final String ERROR_WALLET_NOT_FOUND = "Wallet no encontrado";
    public static final String ERROR_WALLET_NOT_FOUND_FOR_USER = "Wallet no encontrado para el usuario";
    public static final String ERROR_WALLET_NOT_ACTIVE = "La wallet no está activa";
    public static final String ERROR_INSUFFICIENT_FUNDS = "Fondos insuficientes";
    public static final String ERROR_INSUFFICIENT_AVAILABLE_FUNDS = "Fondos disponibles insuficientes";
    public static final String ERROR_INSUFFICIENT_RESERVED_FUNDS = "Fondos reservados insuficientes";
    public static final String ERROR_AMOUNT_MUST_BE_POSITIVE = "El monto debe ser mayor que cero";
    public static final String ERROR_NO_RESERVE_FOUND = "No se encontró operación de reserva para el pago";
    public static final String ERROR_PAYMENT_ALREADY_CONFIRMED = "El pago ya fue confirmado";
    public static final String ERROR_CONCURRENT_UPDATE = "Actualización concurrente detectada, por favor reintente";

    // Mensajes de éxito
    public static final String SUCCESS_WALLET_CREATED = "Wallet creada exitosamente";
    public static final String SUCCESS_FUNDS_RESERVED = "Fondos reservados exitosamente";
    public static final String SUCCESS_FUNDS_RELEASED = "Fondos liberados exitosamente";
    public static final String SUCCESS_DEBIT_CONFIRMED = "Débito confirmado exitosamente";
    public static final String SUCCESS_CREDIT_APPLIED = "Crédito aplicado exitosamente";

    // Configuración por defecto
    public static final String DEFAULT_CURRENCY = "USD";
}
