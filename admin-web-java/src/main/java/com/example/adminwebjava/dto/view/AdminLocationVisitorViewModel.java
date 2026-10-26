package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLocationVisitorViewModel {
    private Integer poiId;
    private String poiName = "";
    private String ownerName = "";
    private Boolean hasMenu;
    private Integer estimatedActiveVisitors;
    private Integer uniqueVisitorsToday;
    private Integer checkInsToday;
    private Integer interactionsToday;
    private Integer menuOrdersToday;
    private Integer pendingOrders;
    private BigDecimal revenueToday;
    private LocalDateTime lastActivityAt;
}