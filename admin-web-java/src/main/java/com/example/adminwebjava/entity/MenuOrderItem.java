package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "menu_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "MenuOrderId", insertable = false, updatable = false)
    private Integer menuOrderId;
    @Column(nullable = false)
    private Integer ownerMenuItemId;
    @Column(nullable = false)
    private String itemName = "";
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(nullable = false)
    private Integer quantity = 1;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
    @Column(nullable = false)
    private String currency = "VND";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MenuOrderId")
    private MenuOrder menuOrder;
}
