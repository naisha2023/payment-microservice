package org.example.authservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.example.shared.dtos.UserResponse;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@EntityListeners(AuditingEntityListener.class)
public class Users {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String role = "CUSTOMER";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false, updatable = true)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean enabled = true;

    public UserResponse toUserResponse() {
        return new UserResponse(
                this.id,
                this.fullName,
                this.email,
                this.role,
                this.status,
                this.createdAt
        );
    }
}