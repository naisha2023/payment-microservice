package org.example.authservice.exception;

public class TokenReusedException extends RuntimeException {
    public TokenReusedException(String message) {
        super(message);
    }
}