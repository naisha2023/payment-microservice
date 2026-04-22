package org.example.authservice.constants;

/**
 * Constantes utilizadas en el servicio de autenticación
 */
public final class AuthConstants {

    private AuthConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no puede ser instanciada");
    }

    // Estados de usuario
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_INACTIVE = "INACTIVE";
    public static final String USER_STATUS_BLOCKED = "BLOCKED";

    // Estados de token
    public static final String TOKEN_STATUS_ACTIVE = "ACTIVE";
    public static final String TOKEN_STATUS_USED = "USED";
    public static final String TOKEN_STATUS_REVOKED = "REVOKED";

    // Tipos de token
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // Mensajes de error
    public static final String ERROR_EMAIL_IN_USE = "El email ya está en uso";
    public static final String ERROR_INVALID_CREDENTIALS = "Credenciales inválidas";
    public static final String ERROR_USER_NOT_FOUND = "Usuario no encontrado";
    public static final String ERROR_TOKEN_NOT_FOUND = "Token no encontrado";
    public static final String ERROR_INVALID_TOKEN_TYPE = "Tipo de token inválido";
    public static final String ERROR_TOKEN_EXPIRED_OR_REVOKED = "Token expirado o revocado";
    public static final String ERROR_TOKEN_INVALID_OR_EXPIRED = "Token inválido o expirado";

    // Mensajes de éxito
    public static final String SUCCESS_USER_REGISTERED = "Usuario registrado exitosamente";
    public static final String SUCCESS_ROLE_CHANGED = "Rol cambiado exitosamente";
    public static final String SUCCESS_LOGOUT = "Sesión cerrada exitosamente";

    // Configuración
    public static final String DEFAULT_CURRENCY = "USD";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7L;
}
