package org.example.authservice.dto;

import java.util.UUID;

public record ChangeRoleRequest(UUID userId, String newRole) {}
