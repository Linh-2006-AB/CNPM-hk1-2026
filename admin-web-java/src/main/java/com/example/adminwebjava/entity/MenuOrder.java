package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String orderCode = "";
    @Column(nullable = false)
    private Integer touristId;

    // Association to Tourist uses the existing touristId column without taking
    // ownership of writes; touristId remains the writable scalar field.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "touristId", referencedColumnName = "id", insertable = false, updatable = false)
    private Tourist tourist;
    @Column(nullable = false)
    private Integer ownerProfileId;
    @Column(nullable = false)
    private Integer poiId;
    @Column(nullable = false)
    private String customerName = "";
    @Column(nullable = false)
    private String customerPhone = "";
    private String note;
    @Column(nullable = false)
    private String status = "Pending";
    @Column(nullable = false)
    private String paymentMethod = "Manual";
    @Column(nullable = false)
    private String paymentStatus = "Pending";
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false)
    private String currency = "VND";
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "menuOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuOrderItem> items = new ArrayList<>();
}
