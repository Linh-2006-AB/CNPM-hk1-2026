package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardAlertViewModel {
    private String icon = "fa-circle-info";
    private String title = "";
    private String description = "";
    private String tone = "info";
    private String url;
}