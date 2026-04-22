package org.example.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import org.example.authservice.exception.InvalidRequestException;
import org.example.authservice.security.CustomUserDetails;

@Service
public class JwtService {

    private static final long ACCESS_TOKEN_EXP = 1000L * 60 * 15;
    private static final long REFRESH_TOKEN_EXP = 1000L * 60 * 60 * 24 * 7;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateAccessToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");

        if (user instanceof CustomUserDetails customUser) {
            claims.put("userId", customUser.getId().toString());
            claims.put("role", customUser.getRole());
        }

        return buildToken(claims, user, ACCESS_TOKEN_EXP);
    }

    public String generateRefreshToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        if (user instanceof CustomUserDetails customUser) {
            claims.put("userId", customUser.getId().toString());
        }

        return buildToken(claims, user, REFRESH_TOKEN_EXP);
    }

    public long getAccessTokenExpiresInSeconds() {
        return ACCESS_TOKEN_EXP / 1000L;
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> (String) claims.get("sub"));
    }

    public Date extractExpiration(String token) {
        Long exp = extractClaim(token, claims -> ((Number) claims.get("exp")).longValue());
        return Date.from(Instant.ofEpochSecond(exp));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> (String) claims.get("type"));
    }

    public <T> T extractClaim(String token, Function<Map<String, Object>, T> claimsResolver) {
        Map<String, Object> claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails user) {
        return user.getUsername().equals(extractUsername(token)) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails user, long expirationMillis) {
    Instant now = Instant.now();
    Map<String, Object> payload = new HashMap<>(extraClaims);
    payload.put("sub", user.getUsername());
    payload.put("jti", UUID.randomUUID().toString());
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusMillis(expirationMillis).getEpochSecond());

    try {
        String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
        String payloadJson = objectMapper.writeValueAsString(payload);

        String encodedHeader = encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String signature = sign(unsignedToken);

        return unsignedToken + "." + signature;
    } catch (JsonProcessingException exception) {
        throw new IllegalStateException("No se pudo construir el JWT", exception);
    }
}

    private Map<String, Object> extractAllClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidRequestException("Token JWT inválido");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidRequestException("Firma JWT inválida");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new InvalidRequestException("No se pudieron leer los claims del JWT");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            return encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar el JWT", exception);
        }
    }

    private String encode(byte[] content) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(content);
    }
}