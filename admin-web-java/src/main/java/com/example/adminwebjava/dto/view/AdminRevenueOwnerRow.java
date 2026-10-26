package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueOwnerRow {

    private Integer ownerProfileId;
    private String businessName = "";
    private Integer completedOrders;
    private BigDecimal grossSales;
    private BigDecimal paidSales;
    private BigDecimal commissionAmount;
    private BigDecimal ownerNetSales;
    private BigDecimal subscriptionPaid;
}