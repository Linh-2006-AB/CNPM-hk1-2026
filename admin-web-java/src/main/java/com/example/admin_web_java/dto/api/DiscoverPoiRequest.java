package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverPoiRequest {
    private Integer poiId;
    private String method = "GPS";
    private String qrCodeToken;
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
}