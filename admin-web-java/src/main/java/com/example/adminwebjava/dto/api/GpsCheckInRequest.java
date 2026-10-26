package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsCheckInRequest {
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
}