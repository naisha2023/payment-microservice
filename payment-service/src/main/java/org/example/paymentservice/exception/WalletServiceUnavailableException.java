package org.example.paymentservice.exception;

public class WalletServiceUnavailableException extends RuntimeException {
    public WalletServiceUnavailableException(String message) {
        super(message);
    }

    public WalletServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
