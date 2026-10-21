package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository {
    Optional findByTokenHash(String tokenHash);

    Optional findTopByEmailOrderByCreatedAtDesc(String email);
}