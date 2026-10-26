package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorTrafficReportViewModel {

    private Integer selectedPoiId;
    private String selectedPoiName = "Tất cả địa điểm / quán";
    private LocalDate date = LocalDate.now();
    private String startTime = "00:00";
    private String endTime = "23:59";
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private List poiOptions = new ArrayList<>();

    private Integer uniqueVisitors;
    private Integer totalEvents;
    private Integer audioInteractions;
    private Integer gpsCheckIns;
    private Integer menuOrders;
    private Integer pendingMenuOrders;
    private BigDecimal menuRevenue;
    private String peakHourLabel = "Chưa có dữ liệu";
    private Integer peakHourVisitors;

    private List hourlyRows = new ArrayList<>();
    private List poiRows = new ArrayList<>();
    private List recentEvents = new ArrayList<>();
}