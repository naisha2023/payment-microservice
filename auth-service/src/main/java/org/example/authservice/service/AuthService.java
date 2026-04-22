package org.example.authservice.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.example.authservice.constants.AuthConstants;
import org.example.authservice.interfaces.AuthServiceInterface;
import org.example.authservice.messaging.UserEventPublisher;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.example.shared.event.UserCreatedEvent;

import org.example.authservice.repository.UserRepository;
import org.example.authservice.security.CustomUserDetails;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RoleResponse;
import org.example.authservice.dto.UserResponse;
import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.RefreshRequest;
import org.example.authservice.entity.RefreshToken;
import org.example.authservice.entity.Users;
import org.example.authservice.enums.Role;
import org.example.authservice.exception.AuthConflictException;
import org.example.authservice.exception.AuthUnauthorizedException;
import org.example.authservice.exception.ResourceNotFoundException;

import org.springframework.transaction.annotation.Transactional;

import org.example.authservice.repository.TokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Servicio de autenticación que maneja registro, login, refresh de tokens y
 * gestión de usuarios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthServiceInterface {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserEventPublisher userEventPublisher;

    /**
     * Registra un nuevo usuario en el sistema
     */
    @Transactional
    public String register(RegisterRequest request) {
        log.info("Iniciando registro de usuario con email: {}", request.email());

        validateEmailNotInUse(request.email());

        Users user = createUserFromRequest(request);
        user = userRepository.save(user);
        log.info("Usuario registrado exitosamente con ID: {}", user.getId());

        publishUserCreatedEvent(user);

        return AuthConstants.SUCCESS_USER_REGISTERED;
    }

    /**
     * Autentica un usuario y genera tokens de acceso
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para email: {}", request.email());

        authenticateUser(request);

        Users user = findUserByEmail(request.email());
        CustomUserDetails userDetails = createCustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        log.info("Login exitoso para usuario: {}", user.getId());
        return new AuthResponse(
                accessToken,
                refreshToken,
                AuthConstants.BEARER_PREFIX.trim(),
                jwtService.getAccessTokenExpiresInSeconds());
    }

    /**
     * Refresca el token de acceso usando un refresh token válido
     */
    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        log.info("Iniciando refresh de token");

        RefreshToken existingToken = findRefreshToken(request.refreshToken());
        validateRefreshToken(existingToken, request.refreshToken());

        revokeRefreshToken(existingToken);

        Users user = existingToken.getUser();
        UserDetails userDetails = toUserDetails(user);

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, newRefreshToken);

        log.info("Token refrescado exitosamente para usuario: {}", user.getId());
        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                AuthConstants.BEARER_PREFIX.trim(),
                jwtService.getAccessTokenExpiresInSeconds());
    }

    /**
     * Obtiene la información del usuario autenticado
     */
    @Transactional(readOnly = true)
    public UserResponse getAuthenticatedUser(String tokenValue) {
        String cleanToken = tokenValue.replace(AuthConstants.BEARER_PREFIX, "").trim();

        validateAccessToken(cleanToken);

        String email = jwtService.extractUsername(cleanToken);
        Users user = findUserByEmail(email);

        if (!jwtService.isTokenValid(cleanToken, toUserDetails(user))) {
            throw new AuthUnauthorizedException(AuthConstants.ERROR_TOKEN_INVALID_OR_EXPIRED);
        }

        return toUserResponse(user);
    }

    /**
     * Cambia el rol de un usuario
     */
    @Transactional
    public String changeRole(UUID userId, String newRole) {
        log.info("Cambiando rol del usuario: {} a: {}", userId, newRole);

        Users user = findUserById(userId);
        user.setRole(newRole);
        userRepository.save(user);

        log.info("Rol cambiado exitosamente para usuario: {}", userId);
        return AuthConstants.SUCCESS_ROLE_CHANGED;
    }

    /**
     * Cierra la sesión del usuario revocando su refresh token
     */
    @Transactional
    public String logout(UUID userId) {
        log.info("Cerrando sesión para usuario: {}", userId);

        Users user = findUserById(userId);
        tokenRepository.findByUser(user).ifPresent(token -> {
            token.setIsRevoked(true);
            token.setStatus(AuthConstants.TOKEN_STATUS_REVOKED);
            tokenRepository.save(token);
        });

        log.info("Sesión cerrada exitosamente para usuario: {}", userId);
        return AuthConstants.SUCCESS_LOGOUT;
    }

    /**
     * Obtiene todos los roles disponibles
     */
    public List<RoleResponse> getAllRoles() {
        return Arrays.stream(Role.values())
                .map(r -> new RoleResponse(r.name(), r.getDescription()))
                .toList();
    }

    /**
     * Busca un usuario por email
     */
    public UserResponse getUserByEmail(String email) {
        Users user = findUserByEmail(email);
        return toUserResponse(user);
    }

    /**
     * Obtiene todos los usuarios del sistema
     */
    @Override
    public Iterable<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(Users::toUserResponse)
                .toList();
    }

    // ==================== Métodos privados de validación ====================

    private void validateEmailNotInUse(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Intento de registro con email duplicado: {}", email);
            throw new AuthConflictException(AuthConstants.ERROR_EMAIL_IN_USE);
        }
    }

    private void validateAccessToken(String token) {
        if (!AuthConstants.TOKEN_TYPE_ACCESS.equals(jwtService.extractTokenType(token))) {
            throw new AuthUnauthorizedException(AuthConstants.ERROR_TOKEN_INVALID_OR_EXPIRED);
        }
    }

    private void validateRefreshToken(RefreshToken token, String jwtToken) {
        if (!AuthConstants.TOKEN_TYPE_REFRESH.equals(jwtService.extractTokenType(jwtToken))) {
            throw new AuthUnauthorizedException(AuthConstants.ERROR_INVALID_TOKEN_TYPE);
        }

        if (token.getIsRevoked()
                || token.getExpiresAt().isBefore(Instant.now())
                || jwtService.isTokenExpired(jwtToken)) {
            throw new AuthUnauthorizedException(AuthConstants.ERROR_TOKEN_EXPIRED_OR_REVOKED);
        }
    }

    // ==================== Métodos privados de creación ====================

    private Users createUserFromRequest(RegisterRequest request) {
        Users user = new Users();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(AuthConstants.USER_STATUS_ACTIVE);
        user.setFullName(request.fullName());
        return user;
    }

    private CustomUserDetails createCustomUserDetails(Users user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isEnabled());
    }

    private UserDetails toUserDetails(Users user) {
        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .build();
    }

    private UserResponse toUserResponse(Users user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    // ==================== Métodos privados de búsqueda ====================

    private Users findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthUnauthorizedException(AuthConstants.ERROR_INVALID_CREDENTIALS));
    }

    private Users findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(AuthConstants.ERROR_USER_NOT_FOUND));
    }

    private RefreshToken findRefreshToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(AuthConstants.ERROR_TOKEN_NOT_FOUND));
    }

    // ==================== Métodos privados de operaciones ====================

    private void authenticateUser(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()));
        } catch (Exception e) {
            log.warn("Fallo de autenticación para email: {}", request.email());
            throw new AuthUnauthorizedException(AuthConstants.ERROR_INVALID_CREDENTIALS);
        }
    }

    private void saveRefreshToken(Users user, String token) {
        RefreshToken refreshToken = tokenRepository.findByUser(user)
                .orElseGet(RefreshToken::new);

        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(
                60L * 60 * 24 * AuthConstants.REFRESH_TOKEN_EXPIRATION_DAYS));
        refreshToken.setIsRevoked(false);
        refreshToken.setStatus(AuthConstants.TOKEN_STATUS_ACTIVE);

        tokenRepository.save(refreshToken);
    }

    private void revokeRefreshToken(RefreshToken token) {
        token.setIsRevoked(true);
        token.setStatus(AuthConstants.TOKEN_STATUS_USED);
        tokenRepository.save(token);
    }

    private void publishUserCreatedEvent(Users user) {
        try {
            userEventPublisher.publish(new UserCreatedEvent(
                    user.getId(),
                    AuthConstants.DEFAULT_CURRENCY));
            log.debug("Evento UserCreated publicado para usuario: {}", user.getId());
        } catch (Exception e) {
            log.error("Error al publicar evento UserCreated para usuario: {}", user.getId(), e);
            // No lanzamos excepción para no afectar el registro
        }
    }
}
