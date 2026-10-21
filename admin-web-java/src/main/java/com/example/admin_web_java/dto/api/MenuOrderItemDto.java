package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrderItemDto {
    private Integer id;
    private Integer menuItemId;
    private String itemName = "";
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private String currency = "VND";
}