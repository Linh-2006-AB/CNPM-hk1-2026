package com.example.adminwebjava.dto.view;

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
public class AdminPurchaseDashboardViewModel {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String rangePreset = "1m";
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDateTime customStart;
    private LocalDateTime customEnd;
    private String rangeLabel = "1 tháng gần nhất";

    private Integer totalPurchases;
    private Integer paidPurchases;
    private Integer pendingPurchases;
    private Integer cancelledPurchases;
    private Integer failedPurchases;

    private Integer ownerPurchaseCount;
    private Integer touristPurchaseCount;
    private Integer ownerPaidCount;
    private Integer touristPaidCount;

    private BigDecimal ownerRevenue;
    private BigDecimal touristRevenue;
    private BigDecimal totalRevenue;

    private Integer activeOwnerSubscriptions;
    private Integer activeTouristSubscriptions;

    private List ownerPlanRows = new ArrayList<>();
    private List touristPlanRows = new ArrayList<>();
    private List topOwnerBuyers = new ArrayList<>();
    private List topTouristBuyers = new ArrayList<>();
    private List last14Days = new ArrayList<>();
    private List recentTransactions = new ArrayList<>();
}