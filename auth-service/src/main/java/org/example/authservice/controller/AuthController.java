package org.example.authservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.dto.*;
import org.example.authservice.security.CustomUserDetails;
import org.example.authservice.service.AuthService;
import org.example.shared.dtos.ApiResponse;
import org.example.shared.dtos.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para operaciones de autenticación y gestión de usuarios
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para autenticación y gestión de usuarios")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar nuevo usuario", description = "Crea una nueva cuenta de usuario en el sistema")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Solicitud de registro recibida para email: {}", request.email());
        String message = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve tokens de acceso")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Solicitud de login recibida para email: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Refrescar token", description = "Genera un nuevo par de tokens usando un refresh token válido")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Solicitud de refresh token recibida");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Obtener información del usuario actual", description = "Devuelve los datos del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> me(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            log.warn("Intento de acceso a /me sin autenticación");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No autenticado", null));
        }

        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole());

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token del usuario")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            log.info("Solicitud de logout para usuario: {}", user.getId());
            String message = authService.logout(user.getId());
            return ResponseEntity.ok(ApiResponse.success(message));
        }

        log.warn("Intento de logout sin autenticación válida");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("No autenticado", null));
    }

    @Operation(summary = "Cambiar rol de usuario", description = "Permite a un administrador cambiar el rol de un usuario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/change-role")
    public ResponseEntity<ApiResponse<String>> changeRole(@Valid @RequestBody ChangeRoleRequest request) {
        log.info("Solicitud de cambio de rol para usuario: {} a rol: {}", request.userId(), request.newRole());
        String message = authService.changeRole(request.userId(), request.newRole());
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Operation(summary = "Buscar usuario por email", description = "Busca y devuelve la información de un usuario por su email")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/findUserByEmail/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> findUserByEmail(@PathVariable String email) {
        log.info("Búsqueda de usuario por email: {}", email);
        UserResponse user = authService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Obtener todos los usuarios", description = "Devuelve la lista completa de usuarios del sistema")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponse<Iterable<UserResponse>>> getAllUsers() {
        log.info("Solicitud de lista de todos los usuarios");
        Iterable<UserResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "Obtener todos los roles", description = "Devuelve la lista de roles disponibles en el sistema")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllRoles")
    public ResponseEntity<ApiResponse<Iterable<RoleResponse>>> getAllRoles() {
        log.info("Solicitud de lista de todos los roles");
        Iterable<RoleResponse> roles = authService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @Operation(summary = "Find user by id", description = "Returns user information by user id")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_SERVICE')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> findUserById(@PathVariable UUID userId) {
        log.info("Finding user by id: {}", userId);
        UserResponse user = authService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/internal/token")
    public ResponseEntity<String> generateServiceToken() {
        String token = authService.generateServiceToken();

        return ResponseEntity.ok(token);
    }

    /**
     * DTO interno para información básica del usuario
     */
    private record UserInfo(UUID userId, String email, String role) {
    }
}
