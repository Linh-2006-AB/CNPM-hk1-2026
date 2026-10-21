package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPurchaseBuyerRow {

    private Integer ownerProfileId;
    private Integer touristId;
    private String displayName = "";
    private String contact = "";
    private Integer paidCount;
    private BigDecimal totalPaid;
    private LocalDateTime lastPaidAt;
}