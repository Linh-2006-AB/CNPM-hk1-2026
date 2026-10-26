package com.example.adminwebjava.dto.view;

import com.example.adminwebjava.entity.MenuOrder;
import com.example.adminwebjava.entity.PaymentTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueDashboardViewModel {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String rangePreset = "1m";
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDateTime customStart;
    private LocalDateTime customEnd;
    private String rangeLabel = "1 tháng gần nhất";
    private BigDecimal platformCommissionRate = new BigDecimal("0.10");
    private BigDecimal platformSubscriptionRevenue;
    private BigDecimal ownerSubscriptionRevenue;
    private BigDecimal touristSubscriptionRevenue;
    private BigDecimal platformCommissionRevenue;
    private BigDecimal platformRevenueTotal;
    private BigDecimal grossMenuSales;
    private BigDecimal completedMenuSales;
    private BigDecimal paidMenuSales;
    private BigDecimal pendingMenuSales;
    private BigDecimal ownerNetMenuRevenue;
    private Integer paidPaymentCount;
    private Integer pendingPaymentCount;
    private Integer completedOrderCount;
    private Integer pendingOrderCount;
    private Integer cancelledOrderCount;

    private List topOwners = new ArrayList<>();
    private List last14Days = new ArrayList<>();
    private List recentPayments = new ArrayList<>();
    private List recentOrders = new ArrayList<>();
}