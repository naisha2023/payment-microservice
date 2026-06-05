package org.example.paymentservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.example.paymentservice.enums.PaymentStatus;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.paymentservice.enums.PaymentType;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String reference;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerPaymentId;

    @Column
    private String qrCode;

    @Column
    private String qrCodeBase64;

    private String description;

    @Column(nullable = false)
    private UUID walletId;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private PaymentType paymentType;

    @Column
    private Instant paidAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markApproved() {
        this.status = PaymentStatus.APPROVED;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
