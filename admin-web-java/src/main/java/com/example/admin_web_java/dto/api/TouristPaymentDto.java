package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristPaymentDto {
    private Integer id;
    private String transactionCode = "";
    private String planName = "";
    private BigDecimal amount;
    private String currency = "VND";
    private String paymentMethod = "";
    private String status = "";
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}