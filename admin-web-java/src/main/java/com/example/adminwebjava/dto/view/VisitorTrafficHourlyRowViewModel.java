package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorTrafficHourlyRowViewModel {

    private Integer hour;
    private String label = "";
    private Integer uniqueVisitors;
    private Integer audioInteractions = 0;
    private Integer gpsCheckIns = 0;
    private Integer menuOrders = 0;
    private BigDecimal menuRevenue;

    public Integer getTotalEvents() {
        return (audioInteractions != null ? audioInteractions : 0) +
               (gpsCheckIns != null ? gpsCheckIns : 0) +
               (menuOrders != null ? menuOrders : 0);
    }
}