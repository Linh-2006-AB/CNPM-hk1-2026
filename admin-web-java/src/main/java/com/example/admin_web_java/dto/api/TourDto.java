package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourDto {
    private Integer id;
    private String title = "";
    private String description = "";
    private Integer durationMinutes;
    private List pois = new ArrayList<>();
}