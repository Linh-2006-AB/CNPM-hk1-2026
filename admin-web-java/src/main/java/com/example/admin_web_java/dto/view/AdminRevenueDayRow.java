package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueDayRow {

    private LocalDateTime date;
    private String label = "";
    private BigDecimal platformRevenue;
    private BigDecimal subscriptionRevenue;
    private BigDecimal commissionRevenue;
    private BigDecimal menuSales;
    private BigDecimal ownerNetSales;
    private Integer orders;
}