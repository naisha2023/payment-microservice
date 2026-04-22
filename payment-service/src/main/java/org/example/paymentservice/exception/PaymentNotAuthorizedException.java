package org.example.paymentservice.exception;

/**
 * Excepción lanzada cuando un usuario intenta acceder a un pago que no le
 * pertenece
 */
public class PaymentNotAuthorizedException extends RuntimeException {
    public PaymentNotAuthorizedException(String message) {
        super(message);
    }
}
