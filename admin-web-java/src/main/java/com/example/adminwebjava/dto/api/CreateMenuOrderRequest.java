package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuOrderRequest {
    private Integer poiId;
    private String customerName;
    private String customerPhone;
    private String note;
    private List items = new ArrayList<>();
}
