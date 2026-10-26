package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardViewModels {
    private Integer totalTourists;
    private Integer activeTouristsToday;
    private Integer estimatedActiveVisitors;
    private Integer checkInsToday;
    private Integer menuOrdersToday;
    private Integer pendingMenuOrders;
    private BigDecimal menuRevenueToday;
    private String peakHourLabel = "Chưa có dữ liệu";
    private String busiestLocationName = "Chưa có dữ liệu";
    private Integer busiestLocationVisitors;
    private List hourlyTraffic = new ArrayList<>();
    private List locationVisitors = new ArrayList<>();
    private List alerts = new ArrayList<>();
}