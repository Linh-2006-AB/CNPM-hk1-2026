package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorTrafficEventRowViewModel {

    private LocalDateTime createdAt;
    private String eventType = "";
    private String poiName = "";
    private String visitorLabel = "";
    private String detail = "";
}