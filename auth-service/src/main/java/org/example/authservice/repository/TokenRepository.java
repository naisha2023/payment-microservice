package org.example.authservice.repository;

import org.example.authservice.entity.RefreshToken;
import org.example.authservice.entity.Users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(Users user);
    List<RefreshToken> findAllByFamily(UUID family);
    void deleteAllByUserAndIsRevokedTrue(Users user);
    List<RefreshToken> findAllByUser(Users user);
}