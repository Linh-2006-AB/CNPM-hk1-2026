package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.TouristSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TouristSubscriptionRepository extends JpaRepository<TouristSubscription, Integer> {
    boolean existsByPaymentTransactionId(Integer paymentTransactionId);
    Optional<TouristSubscription> findFirstByTouristIdAndStatusOrderByExpiresAtDesc(Integer touristId, String status);
}
