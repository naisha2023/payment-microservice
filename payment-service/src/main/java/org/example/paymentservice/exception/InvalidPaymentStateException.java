package org.example.paymentservice.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación en un estado de
 * pago inválido
 */
public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
