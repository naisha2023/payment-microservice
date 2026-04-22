package org.example.authservice.interfaces;

import org.example.authservice.dto.*;

import java.util.List;
import java.util.UUID;

public interface AuthServiceInterface {
    public String register(RegisterRequest request);
    public AuthResponse login(LoginRequest request);
    public AuthResponse refreshToken(RefreshRequest request);
    public String changeRole(UUID userId, String newRole);
    public String logout(UUID userId);
    public UserResponse getUserByEmail(String email);
    public Iterable<UserResponse> getAllUsers();
    public List<RoleResponse> getAllRoles();
}