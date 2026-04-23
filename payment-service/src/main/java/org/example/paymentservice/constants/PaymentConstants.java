package org.example.paymentservice.constants;

/**
 * Constantes utilizadas en el servicio de pagos
 */
public final class PaymentConstants {

    private PaymentConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    // Mensajes de error
    public static final String ERROR_PAYMENT_NOT_FOUND = "Pago no encontrado";
    public static final String ERROR_PAYMENT_NOT_AUTHORIZED = "No tienes autorización para acceder a este pago";
    public static final String ERROR_PAYMENT_NOT_RESERVED = "El pago no está en estado RESERVED";
    public static final String ERROR_PAYMENT_ALREADY_COMPLETED = "No puedes cancelar un pago completado";
    public static final String ERROR_PROCESSING_PAYMENT = "Error procesando pago";
    public static final String ERROR_PAYMENT_CONFIRMATION = "Error confirming payment";

    // Mensajes de éxito
    public static final String SUCCESS_PAYMENT_CREATED = "Pago creado exitosamente";
    public static final String SUCCESS_PAYMENT_CONFIRMED = "Pago confirmado exitosamente";
    public static final String SUCCESS_PAYMENT_CANCELLED = "Pago cancelado exitosamente";    
    
    // Configuración
    public static final int MAX_FAILURE_REASON_LENGTH = 255;
    public static final String OUTBOX_AGGREGATE_TYPE = "PAYMENT";
    public static final String EVENT_PAYMENT_CREATED = "PAYMENT_CREATED";
    public static final String EVENT_PAYMENT_COMPLETED = "payment.completed";
    public static final String EVENT_MESSAGE_SEND = "payment.message.send";
}
