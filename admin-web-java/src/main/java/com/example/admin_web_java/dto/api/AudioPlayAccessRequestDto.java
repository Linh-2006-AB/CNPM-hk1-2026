package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioPlayAccessRequestDto {
    private Integer poiId;
    private String languageCode = "vi";
    private String deviceId = "";
    private Boolean isTts;
}