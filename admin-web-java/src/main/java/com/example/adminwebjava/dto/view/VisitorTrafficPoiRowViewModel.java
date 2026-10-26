package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorTrafficPoiRowViewModel {

    private Integer poiId;
    private String poiName = "";
    private String ownerName = "";
    private Boolean hasMenu;
    private Integer uniqueVisitors;
    private Integer audioInteractions = 0;
    private Integer gpsCheckIns = 0;
    private Integer menuOrders = 0;
    private Integer pendingMenuOrders;
    private BigDecimal menuRevenue;
    private LocalDateTime lastActivityAt;

    public Integer getTotalEvents() {
        return (audioInteractions != null ? audioInteractions : 0) +
               (gpsCheckIns != null ? gpsCheckIns : 0) +
               (menuOrders != null ? menuOrders : 0);
    }
}