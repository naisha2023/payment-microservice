package org.example.authservice.dto;

public record RegisterRequest(String email, String password, String fullName) {

}
