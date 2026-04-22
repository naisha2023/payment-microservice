package org.example.authservice.entity;

import java.time.Instant; // Mejor para manejar tiempos exactos (UTC)
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_tokens") // Es buena práctica usar plural para tablas
public class RefreshToken { // CamelCase en el nombre de la clase (estándar Java)

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // Relación directa con tu entidad Users
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Boolean isRevoked = false;
}
