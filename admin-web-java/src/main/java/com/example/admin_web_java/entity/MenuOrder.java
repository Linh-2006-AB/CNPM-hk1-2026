package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PaymentPlans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String planCode = "";

    @Column(nullable = false)
    private String planName = "";

    @Column(nullable = false)
    private String audience = "Owner";

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer durationDays = 30;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL)
    private List payments = new ArrayList<>();

    @OneToMany(mappedBy = "paymentPlan", cascade = CascadeType.ALL)
    private List ownerSubscriptions = new ArrayList<>();
}