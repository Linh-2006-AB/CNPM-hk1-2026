package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPurchaseDayRow {

    private LocalDateTime date;
    private String label = "";
    private Integer ownerPurchases;
    private Integer touristPurchases;
    private BigDecimal ownerRevenue;
    private BigDecimal touristRevenue;
}