package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;

import feign.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserId(UUID userId);

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerPaymentId = :providerPaymentId")
    Optional<Payment> findByProviderPaymentIdForUpdate(
       @Param("providerPaymentId") String providerPaymentId
    );
}