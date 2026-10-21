package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminHourlyTrafficViewModel {
    private Integer hour;
    private String label = "";
    private Integer checkIns;
    private Integer interactions;
    private Integer menuOrders;
    private Integer uniqueVisitors;
}