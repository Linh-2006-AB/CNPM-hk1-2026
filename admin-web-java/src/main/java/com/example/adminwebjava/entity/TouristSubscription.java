package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "tourist_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "TouristId", nullable = false)
    private Integer touristId;

    @Column(name = "PaymentPlanId", nullable = false)
    private Integer paymentPlanId;

    @Column(name = "PaymentTransactionId")
    private Integer paymentTransactionId;

    @Column(name = "Status", nullable = false)
    private String status = "Active";

    @Column(name = "StartsAt", nullable = false)
    private Instant startsAt;

    @Column(name = "ExpiresAt", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TouristId", insertable = false, updatable = false)
    private Tourist tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentPlanId", insertable = false, updatable = false)
    private PaymentPlan paymentPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentTransactionId", insertable = false, updatable = false)
    private PaymentTransaction paymentTransaction;
}
