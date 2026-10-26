package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPoiDto {
    private Integer id;
    private Integer sequenceOrder;
    private String name = "";
    private String shortDescription = "";
    private String fullDescription = "";
    private String audioUrl;
    private String videoUrl;
    private String coverImageUrl;
    private Double latitude;
    private Double longitude;
    private Double radius;
}