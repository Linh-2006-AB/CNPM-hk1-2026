package com.example.adminwebjava.dto.api;

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
public class MenuOrderDto {
    private Integer id;
    private String orderCode = "";
    private Integer poiId;
    private String poiName = "";
    private String ownerName = "";
    private String customerName = "";
    private String customerPhone = "";
    private String status = "";
    private String paymentMethod = "";
    private String paymentStatus = "";
    private BigDecimal totalAmount;
    private String currency = "VND";
    private String note;
    private LocalDateTime createdAt;
    private List items = new ArrayList<>();
}