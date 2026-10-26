package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.OwnerSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerSubscriptionRepository extends JpaRepository<OwnerSubscription, Integer> {
    boolean existsByPaymentTransactionId(Integer paymentTransactionId);
    Optional<OwnerSubscription> findFirstByOwnerProfileIdAndStatusOrderByExpiresAtDesc(Integer ownerProfileId, String status);
}
